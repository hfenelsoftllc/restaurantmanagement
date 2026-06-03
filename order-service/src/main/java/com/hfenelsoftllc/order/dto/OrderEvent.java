package com.hfenelsoftllc.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Kafka message payload published to ORDERTOPIC.
 * Contains an HMAC-SHA256 signature for message integrity verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /** HMAC-SHA256 base64url-encoded signature (excluded from signing payload) */
    @JsonProperty("signature")
    private String signature;

    /** Always "HS256" */
    @JsonProperty("signature_algorithm")
    private String signatureAlgorithm;

    /** Epoch-seconds expiry — consumer must reject messages past this time */
    @JsonProperty("expires_at")
    private Long expiresAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEvent implements Serializable {

        @JsonProperty("food_item_id")
        private Long foodItemId;

        @JsonProperty("quantity")
        private Integer quantity;

        @JsonProperty("price")
        private BigDecimal price;
    }
}

