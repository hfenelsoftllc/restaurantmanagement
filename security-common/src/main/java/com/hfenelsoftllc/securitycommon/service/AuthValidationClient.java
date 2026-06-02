package com.hfenelsoftllc.securitycommon.service;

import com.hfenelsoftllc.securitycommon.config.AuthValidationProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public class AuthValidationClient {
    private final RestClient restClient;
    private final AuthValidationProperties authValidationProperties;

    public AuthValidationClient(RestClient restClient, AuthValidationProperties authValidationProperties) {
        this.restClient = restClient;
        this.authValidationProperties = authValidationProperties;
    }

    public void validateAuthorizationHeader(String authorizationHeader) {
        if (!authValidationProperties.isEnabled()) {
            return;
        }

        try {
            restClient.get()
                    .uri(authValidationProperties.getBaseUrl() + "/users/token/validate")
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new IllegalArgumentException("Token is invalid, expired, or rotated");
                    })
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException("Token is invalid, expired, or rotated");
        }
    }

    public boolean isTokenValid(String bearerToken) {
        try {
            String authorizationHeader = bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken;
            validateAuthorizationHeader(authorizationHeader);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
