package com.hfenelsoftllc.order.service;

import com.hfenelsoftllc.order.dto.OrderEvent;
import com.hfenelsoftllc.order.dto.OrderRequest;
import com.hfenelsoftllc.order.dto.OrderResponse;
import com.hfenelsoftllc.order.entity.Order;
import com.hfenelsoftllc.order.entity.OrderItem;
import com.hfenelsoftllc.order.exception.InvalidOrderOperationException;
import com.hfenelsoftllc.order.exception.OrderNotFoundException;
import com.hfenelsoftllc.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;

    public OrderService(OrderRepository orderRepository, OrderEventProducer eventProducer) {
        this.orderRepository = orderRepository;
        this.eventProducer = eventProducer;
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    public OrderResponse createOrder(OrderRequest request, Long userId) {
        // Calculate total
        BigDecimal total = request.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String correlationId = UUID.randomUUID().toString();

        Order order = Order.builder()
                .userId(userId)
                .restaurantId(request.getRestaurantId())
                .totalAmount(total)
                .orderStatus("PENDING")
                .paymentStatus("UNPAID")
                .correlationId(correlationId)
                .build();

        // Build order items
        List<OrderItem> items = request.getItems().stream()
                .map(req -> OrderItem.builder()
                        .order(order)
                        .foodItemId(req.getFoodItemId())
                        .quantity(req.getQuantity())
                        .price(req.getPrice())
                        .build())
                .collect(Collectors.toList());
        order.setItems(items);

        Order saved = orderRepository.save(order);
        log.info("Order persisted. orderId={}, correlationId={}, userId={}, total={}",
                saved.getOrderId(), correlationId, userId, total);

        // Publish to Kafka — fire-and-forget with async callback
        OrderEvent event = toOrderEvent(saved, request);
        eventProducer.publishOrderEvent(event);

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId, Long userId) {
        return toResponse(findOrder(orderId, userId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(this::toResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    public OrderResponse updateOrderStatus(Long orderId, String newStatus, Long userId) {
        Order order = findOrder(orderId, userId);

        if (!isValidTransition(order.getOrderStatus(), newStatus)) {
            throw new InvalidOrderOperationException(
                    "Cannot transition order from " + order.getOrderStatus() + " to " + newStatus);
        }

        order.setOrderStatus(newStatus);
        Order updated = orderRepository.save(order);

        log.info("Order status updated. orderId={}, correlationId={}, status={}",
                orderId, order.getCorrelationId(), newStatus);
        return toResponse(updated);
    }

    // -------------------------------------------------------------------------
    // Cancel
    // -------------------------------------------------------------------------

    public void cancelOrder(Long orderId, Long userId) {
        Order order = findOrder(orderId, userId);

        if ("PAID".equals(order.getPaymentStatus())) {
            throw new InvalidOrderOperationException("Cannot cancel a paid order");
        }
        if ("CANCELLED".equals(order.getOrderStatus())) {
            throw new InvalidOrderOperationException("Order is already cancelled");
        }

        order.setOrderStatus("CANCELLED");
        orderRepository.save(order);

        log.info("Order cancelled. orderId={}, correlationId={}", orderId, order.getCorrelationId());
    }

    // -------------------------------------------------------------------------
    // Payment status update (called by internal consumer via REST callback)
    // -------------------------------------------------------------------------

    public void updatePaymentStatus(Long orderId, String paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.setPaymentStatus(paymentStatus);
        if ("PAID".equals(paymentStatus)) {
            order.setOrderStatus("PAID");
        } else if ("FAILED".equals(paymentStatus)) {
            order.setOrderStatus("PENDING"); // allow retry
        }
        orderRepository.save(order);
        log.info("Payment status updated. orderId={}, paymentStatus={}", orderId, paymentStatus);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Order findOrder(Long orderId, Long userId) {
        return orderRepository.findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private boolean isValidTransition(String current, String next) {
        return switch (current) {
            case "PENDING" -> "PAID".equals(next) || "CANCELLED".equals(next);
            case "PAID"    -> "CANCELLED".equals(next);
            default        -> false;
        };
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
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .restaurantId(order.getRestaurantId())
                .items(eventItems)
                .totalAmount(order.getTotalAmount())
                .orderTimestamp(LocalDateTime.now())
                .build();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(i -> OrderResponse.OrderItemResponse.builder()
                        .itemId(i.getItemId())
                        .foodItemId(i.getFoodItemId())
                        .quantity(i.getQuantity())
                        .price(i.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getOrderId())
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
}

