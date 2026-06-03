package com.hfenelsoftllc.order.service;

import com.hfenelsoftllc.order.dto.OrderRequest;
import com.hfenelsoftllc.order.dto.OrderResponse;
import com.hfenelsoftllc.order.entity.Order;
import com.hfenelsoftllc.order.entity.OrderItem;
import com.hfenelsoftllc.order.exception.InvalidOrderOperationException;
import com.hfenelsoftllc.order.exception.OrderNotFoundException;
import com.hfenelsoftllc.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderEventProducer eventProducer;

    @InjectMocks private OrderService orderService;

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
                .orderId(100L)
                .userId(1L)
                .restaurantId(1L)
                .totalAmount(BigDecimal.valueOf(25.00))
                .orderStatus("PENDING")
                .paymentStatus("UNPAID")
                .correlationId(UUID.randomUUID().toString())
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ------------------------------------------------------------------
    // createOrder tests
    // ------------------------------------------------------------------

    @Test
    void createOrder_validRequest_persistsAndPublishes() {
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.createOrder(validRequest, 1L);

        assertThat(response.getOrderId()).isEqualTo(100L);
        assertThat(response.getOrderStatus()).isEqualTo("PENDING");
        assertThat(response.getPaymentStatus()).isEqualTo("UNPAID");

        verify(orderRepository).save(any(Order.class));
        verify(eventProducer).publishOrderEvent(any());
    }

    @Test
    void createOrder_calculatesCorrectTotal() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            // Total should be 2 × 12.50 = 25.00
            assertThat(o.getTotalAmount()).isEqualByComparingTo("25.00");
            return savedOrder;
        });

        orderService.createOrder(validRequest, 1L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_generatesCorrelationId() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            assertThat(o.getCorrelationId()).isNotBlank();
            return savedOrder;
        });

        orderService.createOrder(validRequest, 1L);
    }

    // ------------------------------------------------------------------
    // getOrder tests
    // ------------------------------------------------------------------

    @Test
    void getOrder_existingOrder_returnsResponse() {
        when(orderRepository.findByOrderIdAndUserId(100L, 1L)).thenReturn(Optional.of(savedOrder));

        OrderResponse response = orderService.getOrder(100L, 1L);

        assertThat(response.getOrderId()).isEqualTo(100L);
        assertThat(response.getCorrelationId()).isEqualTo(savedOrder.getCorrelationId());
    }

    @Test
    void getOrder_nonExistingOrder_throwsNotFoundException() {
        when(orderRepository.findByOrderIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(999L, 1L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // cancelOrder tests
    // ------------------------------------------------------------------

    @Test
    void cancelOrder_pendingOrder_cancels() {
        savedOrder.setPaymentStatus("UNPAID");
        savedOrder.setOrderStatus("PENDING");
        when(orderRepository.findByOrderIdAndUserId(100L, 1L)).thenReturn(Optional.of(savedOrder));

        assertThatNoException().isThrownBy(() -> orderService.cancelOrder(100L, 1L));
        verify(orderRepository).save(savedOrder);
        assertThat(savedOrder.getOrderStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelOrder_paidOrder_throwsException() {
        savedOrder.setPaymentStatus("PAID");
        when(orderRepository.findByOrderIdAndUserId(100L, 1L)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(100L, 1L))
                .isInstanceOf(InvalidOrderOperationException.class)
                .hasMessageContaining("paid");
    }

    @Test
    void cancelOrder_alreadyCancelled_throwsException() {
        savedOrder.setOrderStatus("CANCELLED");
        when(orderRepository.findByOrderIdAndUserId(100L, 1L)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(100L, 1L))
                .isInstanceOf(InvalidOrderOperationException.class);
    }

    // ------------------------------------------------------------------
    // updateOrderStatus tests
    // ------------------------------------------------------------------

    @Test
    void updateStatus_pendingToPaid_succeeds() {
        when(orderRepository.findByOrderIdAndUserId(100L, 1L)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(savedOrder)).thenReturn(savedOrder);

        orderService.updateOrderStatus(100L, "PAID", 1L);

        assertThat(savedOrder.getOrderStatus()).isEqualTo("PAID");
    }

    @Test
    void updateStatus_invalidTransition_throwsException() {
        savedOrder.setOrderStatus("CANCELLED");
        when(orderRepository.findByOrderIdAndUserId(100L, 1L)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatus(100L, "PAID", 1L))
                .isInstanceOf(InvalidOrderOperationException.class);
    }
}

