package com.hfenelsoftllc.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class ExternalPaymentProcessingService {

    private final RestClient restClient;

    @Value("${order.service.base-url:http://localhost:9096}")
    private String orderServiceBaseUrl;

    @Value("${payment.provider:stripe}")
    private String provider;

    public ExternalPaymentProcessingService() {
        this.restClient = RestClient.builder().build();
    }

    public void processExternalPayment(OrderEvent event) {
        // Simulate third-party processor call (Stripe/PayPal)
        log.info("External payment processing started. provider={}, orderId={}, correlationId={}, amount={}",
                provider, event.getOrderId(), event.getCorrelationId(), event.getTotalAmount());

        // In real implementation this would call provider SDK/API and verify webhook callbacks
        boolean success = true;

        String status = success ? "PAID" : "FAILED";
        updateOrderPaymentStatus(event.getOrderId(), status);

        log.info("External payment processing finished. provider={}, orderId={}, status={}",
                provider, event.getOrderId(), status);
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

