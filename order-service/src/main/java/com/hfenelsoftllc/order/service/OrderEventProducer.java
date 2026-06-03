package com.hfenelsoftllc.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hfenelsoftllc.order.dto.OrderEvent;
import com.hfenelsoftllc.order.entity.OrderEventLog;
import com.hfenelsoftllc.order.entity.OrderEventLogKey;
import com.hfenelsoftllc.order.dto.OrderEvent;
import com.hfenelsoftllc.order.entity.OrderEventLog;
import com.hfenelsoftllc.order.entity.OrderEventLogKey;
import com.hfenelsoftllc.order.repository.OrderEventLogRepository;
import com.hfenelsoftllc.securitycommon.config.SharedJwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

/**
 * Publishes signed {@link OrderEvent} messages to the Kafka ORDERTOPIC.
 *
 * <p>Each message is:</p>
 * <ul>
 *   <li>Keyed by {@code correlationId} for ordered delivery per order</li>
 *   <li>Signed with HMAC-SHA256 using the shared JWT secret</li>
 *   <li>Stamped with an expiry (default 5 minutes) to prevent replay attacks</li>
 * </ul>
 */
@Slf4j
@Service
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OrderEventLogRepository eventLogRepository;
    private final byte[] signingKey;

    @Value("${kafka.topic.orders:ORDERTOPIC}")
    private String orderTopic;

    @Value("${order.message.signing-enabled:true}")
    private boolean signingEnabled;

    @Value("${order.message.expiration-minutes:5}")
    private long expirationMinutes;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper,
                              OrderEventLogRepository eventLogRepository,
                              SharedJwtProperties jwtProperties) {
        this.kafkaTemplate      = kafkaTemplate;
        this.objectMapper       = objectMapper;
        this.eventLogRepository = eventLogRepository;
        this.signingKey = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Serialise, sign, and publish an {@link OrderEvent} to Kafka.
     */
    public void publishOrderEvent(OrderEvent event) {
        try {
            // Ensure mandatory fields
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }
            if (event.getOrderTimestamp() == null) {
                event.setOrderTimestamp(java.time.LocalDateTime.now());
            }

            // Set expiration
            event.setExpiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES).getEpochSecond());

            // Sign
            if (signingEnabled) {
                String sig = hmacSha256(buildSigningPayload(event));
                event.setSignature(sig);
                event.setSignatureAlgorithm("HS256");
            }

            String json = objectMapper.writeValueAsString(event);

            Message<String> message = MessageBuilder
                    .withPayload(json)
                    .setHeader(KafkaHeaders.TOPIC, orderTopic)
                    .setHeader(KafkaHeaders.KEY, event.getCorrelationId())
                    .setHeader("event-id",       event.getEventId())
                    .setHeader("correlation-id", event.getCorrelationId())
                    .setHeader("x-timestamp",    System.currentTimeMillis())
                    .build();

            final String orderId       = event.getOrderId();
            final String eventId       = event.getEventId();
            final String correlationId = event.getCorrelationId();
            final String payload       = json;

            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Order event PRODUCED. correlationId={}, eventId={}, topic={}, partition={}, offset={}",
                            correlationId, eventId,
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    writeEventLog(orderId, eventId, "ORDER_CREATED", "PRODUCED",
                            correlationId, payload, null);
                } else {
                    log.error("Order event FAILED. correlationId={}, eventId={}", correlationId, eventId, ex);
                    writeEventLog(orderId, eventId, "ORDER_CREATED", "FAILED",
                            correlationId, payload, ex.getMessage());
                }
            });


        } catch (JsonProcessingException e) {
            log.error("JSON serialisation failed for order event correlationId={}", event.getCorrelationId(), e);
            throw new RuntimeException("Failed to serialise order event", e);
        }
    }

    /**
     * Verifies a received {@link OrderEvent} signature — called by consumers to validate integrity.
     */
    public boolean verifySignature(OrderEvent event) {
        if (event.getSignature() == null || event.getSignature().isBlank()) {
            return false;
        }
        try {
            String expected = hmacSha256(buildSigningPayload(event));
            // Constant-time comparison to prevent timing attacks
            return java.security.MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    event.getSignature().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void writeEventLog(String orderIdStr, String eventId, String eventType,
                                String eventStatus, String correlationId,
                                String payload, String errorMessage) {
        try {
            UUID orderId = UUID.fromString(orderIdStr);
            OrderEventLog entry = OrderEventLog.builder()
                    .key(new OrderEventLogKey(orderId, eventId))
                    .eventType(eventType)
                    .eventStatus(eventStatus)
                    .correlationId(correlationId)
                    .payload(payload)
                    .errorMessage(errorMessage)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            eventLogRepository.save(entry);
        } catch (Exception logEx) {
            log.warn("Failed to write event log for orderId={}. {}", orderIdStr, logEx.getMessage());
        }
    }


    /**
     * Build a canonical payload string from the event, excluding the signature field itself.
     */
    private String buildSigningPayload(OrderEvent event) throws JsonProcessingException {
        OrderEvent copy = OrderEvent.builder()
                .eventId(event.getEventId())
                .correlationId(event.getCorrelationId())
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .restaurantId(event.getRestaurantId())
                .items(event.getItems())
                .totalAmount(event.getTotalAmount())
                .orderTimestamp(event.getOrderTimestamp())
                .expiresAt(event.getExpiresAt())
                .build();
        return objectMapper.writeValueAsString(copy);
    }

    private String hmacSha256(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("HMAC-SHA256 signing failed", e);
        }
    }
}

