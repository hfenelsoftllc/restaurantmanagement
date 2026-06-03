package com.hfenelsoftllc.order.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hfenelsoftllc.order.dto.OrderEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lightweight integration-style checks for the OrderEvent payload contract.
 *
 * <p>Verifies JSON round-trip fidelity after the Apache Cassandra migration where
 * {@code orderId} changed from {@code Long} to {@code String} (UUID representation).</p>
 */
class OrderKafkaIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("OrderEvent JSON round-trip — all fields preserved with UUID orderId")
    void orderEventJsonRoundTrip() throws Exception {
        String orderId = UUID.randomUUID().toString(); // orderId is now a UUID string

        OrderEvent event = OrderEvent.builder()
                .eventId("evt-1")
                .correlationId("corr-1")
                .orderId(orderId)                       // String, not Long
                .userId(10L)
                .restaurantId(1L)
                .totalAmount(BigDecimal.valueOf(25.00))
                .orderTimestamp(LocalDateTime.of(2026, 6, 1, 12, 30, 0))
                .signature("sig")
                .signatureAlgorithm("HS256")
                .expiresAt(1_700_000_000L)
                .items(List.of(
                        OrderEvent.OrderItemEvent.builder()
                                .foodItemId(5L)
                                .quantity(2)
                                .price(BigDecimal.valueOf(12.50))
                                .build()
                ))
                .build();

        String json = objectMapper.writeValueAsString(event);
        OrderEvent restored = objectMapper.readValue(json, OrderEvent.class);

        assertThat(restored.getEventId()).isEqualTo("evt-1");
        assertThat(restored.getCorrelationId()).isEqualTo("corr-1");
        assertThat(restored.getOrderId()).isEqualTo(orderId);       // String UUID comparison
        assertThat(restored.getItems()).hasSize(1);
        assertThat(restored.getItems().get(0).getFoodItemId()).isEqualTo(5L);
        assertThat(restored.getSignatureAlgorithm()).isEqualTo("HS256");
        assertThat(restored.getExpiresAt()).isEqualTo(1_700_000_000L);
    }

    @Test
    @DisplayName("OrderEvent JSON — order_id field serialises as string (not numeric)")
    void orderEventJson_orderIdIsStringNotNumeric() throws Exception {
        String orderId = UUID.randomUUID().toString();
        OrderEvent event = OrderEvent.builder()
                .orderId(orderId)
                .correlationId("corr-2")
                .userId(1L)
                .restaurantId(1L)
                .totalAmount(BigDecimal.ONE)
                .items(List.of())
                .build();

        String json = objectMapper.writeValueAsString(event);

        // The JSON must contain the UUID as a quoted string, e.g. "order_id":"<uuid>"
        assertThat(json).contains("\"order_id\":\"" + orderId + "\"");
    }
}
