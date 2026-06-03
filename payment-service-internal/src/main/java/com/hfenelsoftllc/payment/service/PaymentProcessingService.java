package com.hfenelsoftllc.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
public class PaymentProcessingService {

    private final RestClient restClient;

    @Value("${order.service.base-url:http://localhost:9096}")
    private String orderServiceBaseUrl;

    public PaymentProcessingService() {
        this.restClient = RestClient.builder().build();
    }

    public void processPayment(OrderEvent event) {
        // Simulate synchronous internal payment gateway processing
        log.info("Internal payment processing started. orderId={}, correlationId={}, amount={}",
                event.getOrderId(), event.getCorrelationId(), event.getTotalAmount());

        // In real implementation, call internal banking/payment platform here
        boolean success = true;

        String status = success ? "PAID" : "FAILED";
        updateOrderPaymentStatus(event.getOrderId(), status);

        log.info("Internal payment processing finished. orderId={}, status={}", event.getOrderId(), status);
    }

    private void updateOrderPaymentStatus(Long orderId, String status) {
        try {
            ResponseEntity<Void> response = restClient.put()
                    .uri(orderServiceBaseUrl + "/api/v1/orders/{orderId}/payment-status?status={status}", orderId, status)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Order payment status callback sent. orderId={}, status={}, httpStatus={}",
                    orderId, status, response.getStatusCode());
        } catch (Exception ex) {
            log.error("Failed to callback order-service for payment status update. orderId={}, status={}",
                    orderId, status, ex);
        }
    }
}

