package com.hfenelsoftllc.securitycommon.service;

import com.hfenelsoftllc.securitycommon.config.AuthValidationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class AuthValidationClient {

    private final RestClient restClient;
    private final AuthValidationProperties properties;

    public AuthValidationClient(RestClient restClient, AuthValidationProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public boolean isTokenValid(String bearerToken) {
        if (!properties.isEnabled()) {
            return true;
        }
        try {
            ResponseEntity<Map> response = restClient.get()
                    .uri(properties.getUrl() + "/api/v1/auth/validate")
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .toEntity(Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception ex) {
            return false;
        }
    }
}

