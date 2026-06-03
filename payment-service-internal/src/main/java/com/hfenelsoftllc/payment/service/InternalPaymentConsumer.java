package com.hfenelsoftllc.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class InternalPaymentConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentProcessingService paymentProcessingService;

    @Value("${kafka.topic.orders:ORDERTOPIC}")
    private String ordersTopic;

    public InternalPaymentConsumer(ObjectMapper objectMapper,
                                   PaymentProcessingService paymentProcessingService) {
        this.objectMapper = objectMapper;
        this.paymentProcessingService = paymentProcessingService;
    }

    @KafkaListener(
            topics = "${kafka.topic.orders:ORDERTOPIC}",
            groupId = "${spring.kafka.consumer.group-id:payment-service-internal-group}",
            concurrency = "3"
    )
    public void consumeOrderEvent(
            @Payload String orderEventJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = "correlation-id", required = false) String correlationId,
            Acknowledgment acknowledgment
    ) {
        try {
            OrderEvent event = objectMapper.readValue(orderEventJson, OrderEvent.class);

            if (isMessageExpired(event)) {
                log.warn("Expired order event skipped. correlationId={}, orderId={}, topic={}, partition={}, offset={}",
                        correlationId, event.getOrderId(), topic, partition, offset);
                acknowledgment.acknowledge();
                return;
            }

            log.info("Internal consumer received event. correlationId={}, orderId={}, topic={}, partition={}, offset={}",
                    correlationId, event.getOrderId(), topic, partition, offset);

            paymentProcessingService.processPayment(event);
            acknowledgment.acknowledge();

        } catch (Exception ex) {
            log.error("Internal payment consumer failed. correlationId={}, topic={}, partition={}, offset={}",
                    correlationId, topic, partition, offset, ex);
            // Do not acknowledge; retry based on consumer policy.
            throw new RuntimeException("Internal payment consumer failed", ex);
        }
    }

    private boolean isMessageExpired(OrderEvent event) {
        return event.getExpiresAt() != null && Instant.now().getEpochSecond() >= event.getExpiresAt();
    }
}
