package com.hfenelsoftllc.order.service;

import com.hfenelsoftllc.order.dto.OrderEvent;
import com.hfenelsoftllc.order.dto.OrderRequest;
import com.hfenelsoftllc.order.dto.OrderResponse;
import com.hfenelsoftllc.order.entity.*;
import com.hfenelsoftllc.order.exception.InvalidOrderOperationException;
import com.hfenelsoftllc.order.exception.OrderNotFoundException;
import com.hfenelsoftllc.order.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core order business logic.
 *
 * <p><b>Cassandra design highlights:</b></p>
 * <ul>
 *   <li>No {@code @Transactional} — Cassandra is not a relational database; consistency
 *       is achieved via logged-batch writes to multiple tables and Lightweight Transactions
 *       (LWT) for optimistic locking.</li>
 *   <li>Order items are embedded as a Cassandra UDT list — no join, no relationship.</li>
 *   <li>Writes to {@code orders}, {@code orders_by_user}, and {@code orders_by_correlation}
 *       are batched for atomicity across denormalised tables.</li>
 *   <li>Every Kafka event state transition is appended to {@code order_event_log}.</li>
 * </ul>
 */
@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrdersByUserRepository ordersByUserRepository;
    private final OrderByCorrelationRepository orderByCorrelationRepository;
    private final OrderEventLogRepository orderEventLogRepository;
    private final OrderEventProducer eventProducer;
    private final CassandraTemplate cassandraOperations;

    public OrderService(OrderRepository orderRepository,
                        OrdersByUserRepository ordersByUserRepository,
                        OrderByCorrelationRepository orderByCorrelationRepository,
                        OrderEventLogRepository orderEventLogRepository,
                        OrderEventProducer eventProducer,
                        CassandraTemplate cassandraOperations) {
        this.orderRepository = orderRepository;
        this.ordersByUserRepository = ordersByUserRepository;
        this.orderByCorrelationRepository = orderByCorrelationRepository;
        this.orderEventLogRepository = orderEventLogRepository;
        this.eventProducer = eventProducer;
        this.cassandraOperations = cassandraOperations;
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    public OrderResponse createOrder(OrderRequest request, Long userId) {
        BigDecimal total = request.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        UUID orderId        = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();
        Instant now          = Instant.now();

        List<OrderItem> items = request.getItems().stream()
                .map(req -> OrderItem.builder()
                        .foodItemId(req.getFoodItemId())
                        .quantity(req.getQuantity())
                        .price(req.getPrice())
                        .build())
                .collect(Collectors.toList());

        // ---- Primary orders table ----
        Order order = Order.builder()
                .orderId(orderId)
                .userId(userId)
                .restaurantId(request.getRestaurantId())
                .totalAmount(total)
                .orderStatus("PENDING")
                .paymentStatus("UNPAID")
                .correlationId(correlationId)
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .items(items)
                .build();

        // ---- Materialized view: orders_by_user ----
        OrdersByUser ordersByUser = OrdersByUser.builder()
                .key(new OrdersByUserKey(userId, now, orderId))
                .restaurantId(request.getRestaurantId())
                .orderStatus("PENDING")
                .paymentStatus("UNPAID")
                .totalAmount(total)
                .correlationId(correlationId)
                .items(items)
                .updatedAt(now)
                .build();

        // ---- Deduplication lookup: orders_by_correlation ----
        OrderByCorrelation byCorrelation = OrderByCorrelation.builder()
                .correlationId(correlationId)
                .orderId(orderId)
                .build();

        // Write all three rows in a single logged batch for consistency
        cassandraOperations.batchOps()
                .insert(order)
                .insert(ordersByUser)
                .insert(byCorrelation)
                .execute();

        log.info("Order persisted to Cassandra. orderId={}, correlationId={}, userId={}, total={}",
                orderId, correlationId, userId, total);

        // Log PRODUCING state before Kafka publish
        appendEventLog(orderId, UUID.randomUUID().toString(), "ORDER_CREATED", "PRODUCING",
                correlationId, null, null);

        // Publish signed Kafka event; the producer updates the log to PRODUCED or FAILED
        OrderEvent event = toOrderEvent(order, request);
        eventProducer.publishOrderEvent(event);

        return toResponse(order);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public OrderResponse getOrder(UUID orderId, Long userId) {
        Order order = findAndVerifyOwner(orderId, userId);
        return toResponse(order);
    }

    public List<OrderResponse> getUserOrders(Long userId, int limit) {
        return ordersByUserRepository.findByUserId(userId, limit).stream()
                .map(this::toResponseFromView)
                .collect(Collectors.toList());
    }

    public List<OrderEventLog> getOrderEventLog(UUID orderId) {
        return orderEventLogRepository.findByOrderId(orderId);
    }

    // -------------------------------------------------------------------------
    // Update status
    // -------------------------------------------------------------------------

    public OrderResponse updateOrderStatus(UUID orderId, String newStatus, Long userId) {
        Order order = findAndVerifyOwner(orderId, userId);

        if (!isValidTransition(order.getOrderStatus(), newStatus)) {
            throw new InvalidOrderOperationException(
                    "Cannot transition order from " + order.getOrderStatus() + " to " + newStatus);
        }

        Instant now = Instant.now();
        order.setOrderStatus(newStatus);
        order.setUpdatedAt(now);
        Order updated = orderRepository.save(order);          // LWT: IF version = :prev

        // Sync materialized view
        updateOrdersByUserStatus(userId, order.getCreatedAt(), orderId, newStatus, order.getPaymentStatus(), now);

        appendEventLog(orderId, UUID.randomUUID().toString(), "ORDER_STATUS_UPDATED", "PRODUCED",
                order.getCorrelationId(), null, null);

        log.info("Order status updated. orderId={}, correlationId={}, status={}",
                orderId, order.getCorrelationId(), newStatus);
        return toResponse(updated);
    }

    // -------------------------------------------------------------------------
    // Cancel
    // -------------------------------------------------------------------------

    public void cancelOrder(UUID orderId, Long userId) {
        Order order = findAndVerifyOwner(orderId, userId);

        if ("PAID".equals(order.getPaymentStatus())) {
            throw new InvalidOrderOperationException("Cannot cancel a paid order");
        }
        if ("CANCELLED".equals(order.getOrderStatus())) {
            throw new InvalidOrderOperationException("Order is already cancelled");
        }

        Instant now = Instant.now();
        order.setOrderStatus("CANCELLED");
        order.setUpdatedAt(now);
        orderRepository.save(order);

        updateOrdersByUserStatus(userId, order.getCreatedAt(), orderId, "CANCELLED", order.getPaymentStatus(), now);

        appendEventLog(orderId, UUID.randomUUID().toString(), "ORDER_CANCELLED", "PRODUCED",
                order.getCorrelationId(), null, null);

        log.info("Order cancelled. orderId={}, correlationId={}", orderId, order.getCorrelationId());
    }

    // -------------------------------------------------------------------------
    // Payment status update (called by payment consumers via REST callback)
    // -------------------------------------------------------------------------

    public void updatePaymentStatus(UUID orderId, String paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId.toString()));

        Instant now = Instant.now();
        order.setPaymentStatus(paymentStatus);
        if ("PAID".equals(paymentStatus)) {
            order.setOrderStatus("PAID");
        } else if ("FAILED".equals(paymentStatus)) {
            order.setOrderStatus("PENDING"); // allow retry
        }
        order.setUpdatedAt(now);
        orderRepository.save(order);

        updateOrdersByUserStatus(order.getUserId(), order.getCreatedAt(), orderId,
                order.getOrderStatus(), paymentStatus, now);

        appendEventLog(orderId, UUID.randomUUID().toString(), "PAYMENT_STATUS_UPDATED", "CONSUMED",
                order.getCorrelationId(), null, null);

        log.info("Payment status updated. orderId={}, paymentStatus={}", orderId, paymentStatus);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Order findAndVerifyOwner(UUID orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId.toString()));
        if (!userId.equals(order.getUserId())) {
            throw new OrderNotFoundException(orderId.toString()); // hide existence from other users
        }
        return order;
    }

    private boolean isValidTransition(String current, String next) {
        return switch (current) {
            case "PENDING" -> "PAID".equals(next) || "CANCELLED".equals(next);
            case "PAID"    -> "CANCELLED".equals(next);
            default        -> false;
        };
    }

    /**
     * Keep the {@code orders_by_user} view in sync after a status change.
     * Cassandra does not have UPDATE with a WHERE on non-primary columns, so we
     * delete the old row and re-insert.  The {@code created_at} clustering key
     * is immutable on an order, so we pass it from the primary row.
     */
    private void updateOrdersByUserStatus(Long userId, Instant createdAt, UUID orderId,
                                          String orderStatus, String paymentStatus, Instant updatedAt) {
        OrdersByUserKey key = new OrdersByUserKey(userId, createdAt, orderId);
        ordersByUserRepository.findById(key).ifPresent(view -> {
            view.setOrderStatus(orderStatus);
            view.setPaymentStatus(paymentStatus);
            view.setUpdatedAt(updatedAt);
            ordersByUserRepository.save(view);
        });
    }

    /** Append an immutable row to the event log. */
    private void appendEventLog(UUID orderId, String eventId, String eventType,
                                 String eventStatus, String correlationId,
                                 String payload, String errorMessage) {
        OrderEventLog log = OrderEventLog.builder()
                .key(new OrderEventLogKey(orderId, eventId))
                .eventType(eventType)
                .eventStatus(eventStatus)
                .correlationId(correlationId)
                .payload(payload)
                .errorMessage(errorMessage)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        orderEventLogRepository.save(log);
    }

    private OrderEvent toOrderEvent(Order order, OrderRequest request) {
        List<OrderEvent.OrderItemEvent> eventItems = request.getItems().stream()
                .map(i -> OrderEvent.OrderItemEvent.builder()
                        .foodItemId(i.getFoodItemId())
                        .quantity(i.getQuantity())
                        .price(i.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(order.getCorrelationId())
                .orderId(order.getOrderId().toString())
                .userId(order.getUserId())
                .restaurantId(order.getRestaurantId())
                .items(eventItems)
                .totalAmount(order.getTotalAmount())
                .orderTimestamp(LocalDateTime.ofInstant(order.getCreatedAt(), ZoneOffset.UTC))
                .build();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                        .map(i -> OrderResponse.OrderItemResponse.builder()
                                .foodItemId(i.getFoodItemId())
                                .quantity(i.getQuantity())
                                .price(i.getPrice())
                                .build())
                        .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getOrderId().toString())
                .userId(order.getUserId())
                .restaurantId(order.getRestaurantId())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .correlationId(order.getCorrelationId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    private OrderResponse toResponseFromView(OrdersByUser view) {
        List<OrderResponse.OrderItemResponse> itemResponses = view.getItems() == null
                ? List.of()
                : view.getItems().stream()
                        .map(i -> OrderResponse.OrderItemResponse.builder()
                                .foodItemId(i.getFoodItemId())
                                .quantity(i.getQuantity())
                                .price(i.getPrice())
                                .build())
                        .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(view.getKey().getOrderId().toString())
                .userId(view.getKey().getUserId())
                .restaurantId(view.getRestaurantId())
                .totalAmount(view.getTotalAmount())
                .orderStatus(view.getOrderStatus())
                .paymentStatus(view.getPaymentStatus())
                .correlationId(view.getCorrelationId())
                .createdAt(view.getKey().getCreatedAt())
                .updatedAt(view.getUpdatedAt())
                .items(itemResponses)
                .build();
    }
}
