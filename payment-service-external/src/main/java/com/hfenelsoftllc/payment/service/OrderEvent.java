package com.hfenelsoftllc.payment.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderEvent {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("restaurant_id")
    private Long restaurantId;

    @JsonProperty("items")
    private List<OrderItemEvent> items;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("order_timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime orderTimestamp;

    @JsonProperty("signature")
    private String signature;

    @JsonProperty("signature_algorithm")
    private String signatureAlgorithm;

    @JsonProperty("expires_at")
    private Long expiresAt;

    @Data
    public static class OrderItemEvent {
        @JsonProperty("food_item_id")
        private Long foodItemId;
        @JsonProperty("quantity")
        private Integer quantity;
        @JsonProperty("price")
        private BigDecimal price;
    }
}

