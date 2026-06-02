package com.hfenelsoftllc.order.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hfenelsoftllc.order.dto.OrderEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lightweight integration-style checks for the event payload contract.
 *
 * <p>Reason: the current workspace's Spring test classpath does not include
 * Spring Boot web test artifacts needed for full random-port integration tests.</p>
 */
class OrderKafkaIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("OrderEvent JSON round-trip preserves expected fields")
    void orderEventJsonRoundTrip() throws Exception {
        OrderEvent event = OrderEvent.builder()
                .eventId("evt-1")
                .correlationId("corr-1")
                .orderId(100L)
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
        assertThat(restored.getOrderId()).isEqualTo(100L);
        assertThat(restored.getItems()).hasSize(1);
        assertThat(restored.getItems().get(0).getFoodItemId()).isEqualTo(5L);
        assertThat(restored.getSignatureAlgorithm()).isEqualTo("HS256");
    }
}
