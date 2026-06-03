package com.hfenelsoftllc.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hfenelsoftllc.order.dto.OrderEvent;
import com.hfenelsoftllc.securitycommon.config.SharedJwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderEventProducerTest {

    @Mock private KafkaTemplate<String, String> kafkaTemplate;
    @Mock private SharedJwtProperties jwtProperties;

    private OrderEventProducer producer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getSecret()).thenReturn("test-secret-key-at-least-32-bytes-long");
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // registers JavaTimeModule

        producer = new OrderEventProducer(kafkaTemplate, objectMapper, jwtProperties);

        // Reflect private fields for test
        org.springframework.test.util.ReflectionTestUtils.setField(producer, "orderTopic", "ORDERTOPIC");
        org.springframework.test.util.ReflectionTestUtils.setField(producer, "signingEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(producer, "expirationMinutes", 5L);

        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(any(org.springframework.messaging.Message.class))).thenReturn(future);
    }

    @Test
    void publishOrderEvent_validEvent_sendsToKafka() {
        OrderEvent event = buildEvent();

        producer.publishOrderEvent(event);

        verify(kafkaTemplate).send(any(org.springframework.messaging.Message.class));
    }

    @Test
    void publishOrderEvent_setsEventIdIfMissing() {
        OrderEvent event = buildEvent();
        event.setEventId(null);

        producer.publishOrderEvent(event);

        assertThat(event.getEventId()).isNotNull();
    }

    @Test
    void publishOrderEvent_setsSignatureWhenEnabled() {
        OrderEvent event = buildEvent();

        producer.publishOrderEvent(event);

        assertThat(event.getSignature()).isNotBlank();
        assertThat(event.getSignatureAlgorithm()).isEqualTo("HS256");
    }

    @Test
    void publishOrderEvent_setsExpiration() {
        OrderEvent event = buildEvent();

        producer.publishOrderEvent(event);

        assertThat(event.getExpiresAt()).isPositive();
        assertThat(event.getExpiresAt()).isGreaterThan(System.currentTimeMillis() / 1000);
    }

    @Test
    void verifySignature_validSignature_returnsTrue() {
        OrderEvent event = buildEvent();
        producer.publishOrderEvent(event);

        assertThat(producer.verifySignature(event)).isTrue();
    }

    @Test
    void verifySignature_tamperedPayload_returnsFalse() {
        OrderEvent event = buildEvent();
        producer.publishOrderEvent(event);

        // Tamper total amount after signing
        event.setTotalAmount(BigDecimal.valueOf(9999.99));

        assertThat(producer.verifySignature(event)).isFalse();
    }

    @Test
    void verifySignature_missingSignature_returnsFalse() {
        OrderEvent event = buildEvent();
        event.setSignature(null);

        assertThat(producer.verifySignature(event)).isFalse();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private OrderEvent buildEvent() {
        return OrderEvent.builder()
                .orderId(1L)
                .userId(42L)
                .restaurantId(5L)
                .correlationId("corr-" + System.nanoTime())
                .totalAmount(BigDecimal.valueOf(25.00))
                .items(List.of(
                        OrderEvent.OrderItemEvent.builder()
                                .foodItemId(10L)
                                .quantity(2)
                                .price(BigDecimal.valueOf(12.50))
                                .build()
                ))
                .build();
    }
}
