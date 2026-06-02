package com.hfenelsoftllc.order.controller;

import com.hfenelsoftllc.order.dto.OrderRequest;
import com.hfenelsoftllc.order.dto.OrderResponse;
import com.hfenelsoftllc.order.dto.OrderStatusUpdateRequest;
import com.hfenelsoftllc.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Slf4j
@Tag(name = "Orders", description = "Order management endpoints")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Create a new order",
               description = "Persists the order and publishes a signed event to Kafka ORDERTOPIC for payment processing")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
            @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal Object principal) {

        Long userId = resolveUserId(principal);
        log.info("Creating order for userId={}, restaurantId={}", userId, request.getRestaurantId());
        OrderResponse response = orderService.createOrder(request, userId);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + response.getOrderId()))
                             .body(response);
    }

    @Operation(summary = "Get order by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Object principal) {

        return ResponseEntity.ok(orderService.getOrder(orderId, resolveUserId(principal)));
    }

    @Operation(summary = "List orders for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getUserOrders(
            @AuthenticationPrincipal Object principal,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getUserOrders(resolveUserId(principal), pageable));
    }

    @Operation(summary = "Update order status",
               description = "Valid transitions: PENDING → PAID | CANCELLED, PAID → CANCELLED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal Object principal) {

        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request.getStatus(), resolveUserId(principal)));
    }

    @Operation(summary = "Cancel an order")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Order cancelled"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Cannot cancel a paid order")
    })
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Object principal) {

        orderService.cancelOrder(orderId, resolveUserId(principal));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update payment status (internal — called by payment consumers)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Payment status updated"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PutMapping("/{orderId}/payment-status")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {

        orderService.updatePaymentStatus(orderId, status);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(Object principal) {
        if (principal == null) {
            throw new IllegalStateException("Missing authenticated principal");
        }
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof String principalValue) {
            return Long.parseLong(principalValue);
        }
        if (principal instanceof UserDetails userDetails) {
            return Long.parseLong(userDetails.getUsername());
        }
        return Long.parseLong(String.valueOf(principal));
    }
}
