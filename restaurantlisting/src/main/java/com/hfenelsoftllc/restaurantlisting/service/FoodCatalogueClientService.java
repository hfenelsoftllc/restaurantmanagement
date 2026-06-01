package com.hfenelsoftllc.restaurantlisting.service;

import com.hfenelsoftllc.restaurantlisting.dto.FoodItemCatalogueDTO;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class FoodCatalogueClientService {
    private final RestClient restClient;
    private final String foodCatalogueBaseUrl;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Bulkhead bulkhead;

    public FoodCatalogueClientService(
            RestClient.Builder loadBalancedRestClientBuilder,
            RestClient.Builder plainRestClientBuilder,
            @Value("${integration.foodcatalogue.base-url:http://FOOD-CATALOGUE-SERVICE}") String foodCatalogueBaseUrl,
            @Value("${integration.foodcatalogue.use-load-balanced:true}") boolean useLoadBalanced,
            @Value("${integration.foodcatalogue.resilience.circuitbreaker.failure-rate-threshold:50}") float cbFailureRateThreshold,
            @Value("${integration.foodcatalogue.resilience.circuitbreaker.minimum-number-of-calls:5}") int cbMinimumNumberOfCalls,
            @Value("${integration.foodcatalogue.resilience.circuitbreaker.sliding-window-size:10}") int cbSlidingWindowSize,
            @Value("${integration.foodcatalogue.resilience.circuitbreaker.wait-duration-in-open-state:15s}") Duration cbWaitDurationInOpenState,
            @Value("${integration.foodcatalogue.resilience.retry.max-attempts:3}") int retryMaxAttempts,
            @Value("${integration.foodcatalogue.resilience.retry.wait-duration:300ms}") Duration retryWaitDuration,
            @Value("${integration.foodcatalogue.resilience.bulkhead.max-concurrent-calls:10}") int bulkheadMaxConcurrentCalls,
            @Value("${integration.foodcatalogue.resilience.bulkhead.max-wait-duration:100ms}") Duration bulkheadMaxWaitDuration
    ) {
        this.restClient = (useLoadBalanced ? loadBalancedRestClientBuilder : plainRestClientBuilder).build();
        this.foodCatalogueBaseUrl = foodCatalogueBaseUrl;

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(cbFailureRateThreshold)
                .minimumNumberOfCalls(cbMinimumNumberOfCalls)
                .slidingWindowSize(cbSlidingWindowSize)
                .waitDurationInOpenState(cbWaitDurationInOpenState)
                .build();
        this.circuitBreaker = CircuitBreakerRegistry.of(circuitBreakerConfig).circuitBreaker("foodCatalogue");

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(retryMaxAttempts)
                .waitDuration(retryWaitDuration)
                .build();
        this.retry = RetryRegistry.of(retryConfig).retry("foodCatalogue");

        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(bulkheadMaxConcurrentCalls)
                .maxWaitDuration(bulkheadMaxWaitDuration)
                .build();
        this.bulkhead = BulkheadRegistry.of(bulkheadConfig).bulkhead("foodCatalogue");
    }

    public List<FoodItemCatalogueDTO> getMenuByRestaurantId(Long restaurantId) {
        try {
            return circuitBreaker.executeSupplier(() ->
                    retry.executeSupplier(() ->
                            bulkhead.executeSupplier(() -> {
                                FoodItemCatalogueDTO[] menuItems = restClient.get()
                                        .uri(foodCatalogueBaseUrl + "/food-items/restaurant/{restaurantId}", restaurantId)
                                        .retrieve()
                                        .body(FoodItemCatalogueDTO[].class);

                                return menuItems == null ? Collections.emptyList() : Arrays.asList(menuItems);
                            })
                    )
            );
        } catch (Exception ex) {
            return fallbackMenu(restaurantId, ex);
        }
    }

    public List<FoodItemCatalogueDTO> fallbackMenu(Long restaurantId, Throwable throwable) {
        return Collections.emptyList();
    }
}

