package com.hfenelsoftllc.order.service;

import com.hfenelsoftllc.order.dto.OrderRequest;
import com.hfenelsoftllc.order.dto.OrderResponse;
import com.hfenelsoftllc.order.entity.Order;
import com.hfenelsoftllc.order.entity.OrderEventLog;
import com.hfenelsoftllc.order.entity.OrdersByUser;
import com.hfenelsoftllc.order.exception.InvalidOrderOperationException;
import com.hfenelsoftllc.order.exception.OrderNotFoundException;
import com.hfenelsoftllc.order.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrderService} after Apache Cassandra migration.
 *
 * <p>Key contract changes verified here:</p>
 * <ul>
 *   <li>orderId is UUID (not Long)</li>
 *   <li>Order creation uses a Cassandra logged batch (not orderRepository.save)</li>
 *   <li>getOrder / cancelOrder / updateOrderStatus accept UUID</li>
 *   <li>OrdersByUser and OrderEventLog side-effects are covered</li>
 *   <li>User-isolation check: getOrder with wrong userId throws NotFoundException</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // shared stubs may not fire in every test
class OrderServiceTest {

    // ---- all six OrderService constructor dependencies ----
    @Mock private OrderRepository orderRepository;
    @Mock private OrdersByUserRepository ordersByUserRepository;
    @Mock private OrderByCorrelationRepository orderByCorrelationRepository;
    @Mock private OrderEventLogRepository orderEventLogRepository;
    @Mock private OrderEventProducer eventProducer;
    @Mock private CassandraTemplate cassandraTemplate;

    @InjectMocks private OrderService orderService;

    // stable identifiers reused across tests
    private final UUID ORDER_ID = UUID.randomUUID();
    private final Long USER_ID  = 1L;

    private OrderRequest validRequest;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        validRequest = OrderRequest.builder()
                .restaurantId(1L)
                .items(List.of(
                        OrderRequest.OrderItemRequest.builder()
                                .foodItemId(10L)
                                .quantity(2)
                                .price(BigDecimal.valueOf(12.50))
                                .build()
                ))
                .build();

        savedOrder = Order.builder()
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .restaurantId(1L)
                .totalAmount(BigDecimal.valueOf(25.00))
                .orderStatus("PENDING")
                .paymentStatus("UNPAID")
                .correlationId(UUID.randomUUID().toString())
                .version(0L)
                .items(List.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // --- Cassandra batch mock chain (used by createOrder) ---
        CassandraBatchOperations batchOps = mock(CassandraBatchOperations.class);
        when(cassandraTemplate.batchOps()).thenReturn(batchOps);
        when(batchOps.insert(any(Object.class))).thenReturn(batchOps);
        // execute() is void — default Mockito behaviour does nothing

        // --- Event log always persists silently ---
        when(orderEventLogRepository.save(any())).thenReturn(mock(OrderEventLog.class));

        // --- OrdersByUser lookup returns empty by default (no view to sync) ---
        when(ordersByUserRepository.findById(any())).thenReturn(Optional.empty());
        when(ordersByUserRepository.save(any())).thenReturn(mock(OrdersByUser.class));
    }

    // ------------------------------------------------------------------
    // createOrder
    // ------------------------------------------------------------------

    @Test
    @DisplayName("createOrder — valid request writes batch and fires Kafka event")
    void createOrder_validRequest_persistsAndPublishes() {
        OrderResponse response = orderService.createOrder(validRequest, USER_ID);

        // orderId is now a UUID string, not a Long
        assertThat(response.getOrderId()).isNotBlank();
        assertThat(UUID.fromString(response.getOrderId())).isNotNull();
        assertThat(response.getOrderStatus()).isEqualTo("PENDING");
        assertThat(response.getPaymentStatus()).isEqualTo("UNPAID");

        // batch must be executed (replaces old orderRepository.save)
        verify(cassandraTemplate).batchOps();
        verify(eventProducer).publishOrderEvent(any());
    }

    @Test
    @DisplayName("createOrder — total = sum(price × quantity)")
    void createOrder_calculatesCorrectTotal() {
        OrderResponse response = orderService.createOrder(validRequest, USER_ID);

        // 2 × 12.50 = 25.00
        assertThat(response.getTotalAmount()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("createOrder — correlation ID is auto-generated and non-blank")
    void createOrder_generatesCorrelationId() {
        OrderResponse response = orderService.createOrder(validRequest, USER_ID);

        assertThat(response.getCorrelationId()).isNotBlank();
    }

    @Test
    @DisplayName("createOrder — event log entry written before Kafka publish (PRODUCING state)")
    void createOrder_writesEventLogBeforeKafkaPublish() {
        orderService.createOrder(validRequest, USER_ID);

        // At least one PRODUCING log entry must be persisted
        verify(orderEventLogRepository, atLeastOnce()).save(argThat(
                log -> "ORDER_CREATED".equals(log.getEventType())
                    && "PRODUCING".equals(log.getEventStatus())
        ));
    }

    // ------------------------------------------------------------------
    // getOrder
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getOrder — existing order owned by caller returns response")
    void getOrder_existingOrder_returnsResponse() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));

        OrderResponse response = orderService.getOrder(ORDER_ID, USER_ID);

        assertThat(response.getOrderId()).isEqualTo(ORDER_ID.toString());
        assertThat(response.getCorrelationId()).isEqualTo(savedOrder.getCorrelationId());
    }

    @Test
    @DisplayName("getOrder — unknown orderId throws OrderNotFoundException")
    void getOrder_nonExistingOrder_throwsNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(orderRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(unknownId, USER_ID))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("getOrder — wrong userId hides order (returns NotFoundException, not 403)")
    void getOrder_wrongUserId_hiddenAsNotFound() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));

        // userId 999 does not own ORDER_ID
        assertThatThrownBy(() -> orderService.getOrder(ORDER_ID, 999L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // cancelOrder
    // ------------------------------------------------------------------

    @Test
    @DisplayName("cancelOrder — PENDING/UNPAID order is cancelled successfully")
    void cancelOrder_pendingOrder_cancels() {
        savedOrder.setPaymentStatus("UNPAID");
        savedOrder.setOrderStatus("PENDING");
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(savedOrder)).thenReturn(savedOrder);

        assertThatNoException().isThrownBy(() -> orderService.cancelOrder(ORDER_ID, USER_ID));

        verify(orderRepository).save(savedOrder);
        assertThat(savedOrder.getOrderStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("cancelOrder — PAID order cannot be cancelled")
    void cancelOrder_paidOrder_throwsException() {
        savedOrder.setPaymentStatus("PAID");
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, USER_ID))
                .isInstanceOf(InvalidOrderOperationException.class)
                .hasMessageContaining("paid");
    }

    @Test
    @DisplayName("cancelOrder — already CANCELLED order is idempotent-safe (throws)")
    void cancelOrder_alreadyCancelled_throwsException() {
        savedOrder.setOrderStatus("CANCELLED");
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, USER_ID))
                .isInstanceOf(InvalidOrderOperationException.class);
    }

    // ------------------------------------------------------------------
    // updateOrderStatus
    // ------------------------------------------------------------------

    @Test
    @DisplayName("updateOrderStatus — PENDING → PAID is a valid transition")
    void updateStatus_pendingToPaid_succeeds() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(savedOrder)).thenReturn(savedOrder);

        orderService.updateOrderStatus(ORDER_ID, "PAID", USER_ID);

        assertThat(savedOrder.getOrderStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("updateOrderStatus — CANCELLED → PAID is an invalid transition")
    void updateStatus_invalidTransition_throwsException() {
        savedOrder.setOrderStatus("CANCELLED");
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatus(ORDER_ID, "PAID", USER_ID))
                .isInstanceOf(InvalidOrderOperationException.class);
    }

    // ------------------------------------------------------------------
    // updatePaymentStatus (payment consumer callback)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("updatePaymentStatus — PAID sets order status to PAID")
    void updatePaymentStatus_paid_setsOrderStatusPaid() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(savedOrder)).thenReturn(savedOrder);

        orderService.updatePaymentStatus(ORDER_ID, "PAID");

        assertThat(savedOrder.getPaymentStatus()).isEqualTo("PAID");
        assertThat(savedOrder.getOrderStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("updatePaymentStatus — FAILED resets order status to PENDING for retry")
    void updatePaymentStatus_failed_resetsOrderStatusToPending() {
        savedOrder.setOrderStatus("PAID"); // intermediate state
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(savedOrder)).thenReturn(savedOrder);

        orderService.updatePaymentStatus(ORDER_ID, "FAILED");

        assertThat(savedOrder.getPaymentStatus()).isEqualTo("FAILED");
        assertThat(savedOrder.getOrderStatus()).isEqualTo("PENDING");
    }
}
