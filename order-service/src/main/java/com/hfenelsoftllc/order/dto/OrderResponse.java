package com.hfenelsoftllc.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {

    private String orderId; // UUID as string
    private Long userId;
    private Long restaurantId;
    private String orderStatus;
    private String paymentStatus;
    private BigDecimal totalAmount;
    private String correlationId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OrderItemResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long foodItemId;
        private Integer quantity;
        private BigDecimal price;
    }
}

