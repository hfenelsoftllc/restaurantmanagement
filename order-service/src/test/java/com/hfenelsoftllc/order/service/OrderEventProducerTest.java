package com.hfenelsoftllc.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hfenelsoftllc.order.dto.OrderEvent;
import com.hfenelsoftllc.order.entity.OrderEventLog;
import com.hfenelsoftllc.order.repository.OrderEventLogRepository;
import com.hfenelsoftllc.securitycommon.config.SharedJwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import org.apache.kafka.clients.producer.RecordMetadata;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrderEventProducer} after the Cassandra migration.
 *
 * <p>Key change: constructor now accepts {@link OrderEventLogRepository} so that
 * PRODUCED / FAILED states are persisted to Cassandra after the async Kafka callback.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderEventProducerTest {

    @Mock private KafkaTemplate<String, String> kafkaTemplate;
    @Mock private OrderEventLogRepository eventLogRepository;
    @Mock private SharedJwtProperties jwtProperties;

    private OrderEventProducer producer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getSecret()).thenReturn("test-secret-key-at-least-32-bytes-long");
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // registers JavaTimeModule

        // Updated constructor: kafkaTemplate, objectMapper, eventLogRepository, jwtProperties
        producer = new OrderEventProducer(kafkaTemplate, objectMapper, eventLogRepository, jwtProperties);

        org.springframework.test.util.ReflectionTestUtils.setField(producer, "orderTopic", "ORDERTOPIC");
        org.springframework.test.util.ReflectionTestUtils.setField(producer, "signingEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(producer, "expirationMinutes", 5L);

        // Kafka send returns a completed future with fully-stubbed RecordMetadata.
        // Without this, result.getRecordMetadata().topic() would NPE inside whenComplete,
        // aborting the callback before writeEventLog(PRODUCED) is called.
        RecordMetadata meta = mock(RecordMetadata.class);
        when(meta.topic()).thenReturn("ORDERTOPIC");
        when(meta.partition()).thenReturn(0);
        when(meta.offset()).thenReturn(0L);

        @SuppressWarnings("unchecked")
        SendResult<String, String> sendResultObj = mock(SendResult.class);
        when(sendResultObj.getRecordMetadata()).thenReturn(meta);

        CompletableFuture<SendResult<String, String>> future =
                CompletableFuture.completedFuture(sendResultObj);
        when(kafkaTemplate.send(any(org.springframework.messaging.Message.class))).thenReturn(future);

        // Event log persistence always succeeds
        when(eventLogRepository.save(any())).thenReturn(mock(OrderEventLog.class));
    }

    @Test
    @DisplayName("publishOrderEvent — sends exactly one message to Kafka")
    void publishOrderEvent_validEvent_sendsToKafka() {
        producer.publishOrderEvent(buildEvent());

        verify(kafkaTemplate).send(any(org.springframework.messaging.Message.class));
    }

    @Test
    @DisplayName("publishOrderEvent — auto-generates eventId when absent")
    void publishOrderEvent_setsEventIdIfMissing() {
        OrderEvent event = buildEvent();
        event.setEventId(null);

        producer.publishOrderEvent(event);

        assertThat(event.getEventId()).isNotNull();
    }

    @Test
    @DisplayName("publishOrderEvent — stamps HMAC-SHA256 signature and algorithm")
    void publishOrderEvent_setsSignatureWhenEnabled() {
        OrderEvent event = buildEvent();

        producer.publishOrderEvent(event);

        assertThat(event.getSignature()).isNotBlank();
        assertThat(event.getSignatureAlgorithm()).isEqualTo("HS256");
    }

    @Test
    @DisplayName("publishOrderEvent — sets expiry in the future (epoch seconds)")
    void publishOrderEvent_setsExpiration() {
        OrderEvent event = buildEvent();

        producer.publishOrderEvent(event);

        assertThat(event.getExpiresAt()).isPositive();
        assertThat(event.getExpiresAt()).isGreaterThan(System.currentTimeMillis() / 1000);
    }

    @Test
    @DisplayName("publishOrderEvent — PRODUCED event log entry written on Kafka success")
    void publishOrderEvent_writesProducedEventLog_onSuccess() {
        producer.publishOrderEvent(buildEvent());

        // The async callback runs synchronously in tests because the future is already complete
        verify(eventLogRepository, atLeastOnce()).save(argThat(
                log -> "PRODUCED".equals(log.getEventStatus())
        ));
    }

    // ------------------------------------------------------------------
    // verifySignature
    // ------------------------------------------------------------------

    @Test
    @DisplayName("verifySignature — signed event passes verification")
    void verifySignature_validSignature_returnsTrue() {
        OrderEvent event = buildEvent();
        producer.publishOrderEvent(event);

        assertThat(producer.verifySignature(event)).isTrue();
    }

    @Test
    @DisplayName("verifySignature — tampered payload fails verification")
    void verifySignature_tamperedPayload_returnsFalse() {
        OrderEvent event = buildEvent();
        producer.publishOrderEvent(event);

        // Tamper total amount after signing
        event.setTotalAmount(BigDecimal.valueOf(9999.99));

        assertThat(producer.verifySignature(event)).isFalse();
    }

    @Test
    @DisplayName("verifySignature — null signature returns false")
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
                .orderId(UUID.randomUUID().toString()) // orderId is now a UUID string
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
