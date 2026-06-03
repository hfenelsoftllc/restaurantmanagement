package com.hfenelsoftllc.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hfenelsoftllc.order.dto.OrderEvent;
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
    private final byte[] signingKey;

    @Value("${kafka.topic.orders:ORDERTOPIC}")
    private String orderTopic;

    @Value("${order.message.signing-enabled:true}")
    private boolean signingEnabled;

    @Value("${order.message.expiration-minutes:5}")
    private long expirationMinutes;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper,
                              SharedJwtProperties jwtProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        // Reuse the same secret as JWT token signing — consistent across services
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

            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Order event published. correlationId={}, eventId={}, topic={}, partition={}, offset={}",
                            event.getCorrelationId(),
                            event.getEventId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish order event. correlationId={}", event.getCorrelationId(), ex);
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

