package com.hfenelsoftllc.securitycommon.service;

public class BearerTokenService {

    public String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Missing Authorization header");
        }

        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix) || authorizationHeader.length() <= prefix.length()) {
            throw new IllegalArgumentException("Authorization header must use Bearer token");
        }

        return authorizationHeader.substring(prefix.length()).trim();
    }
}

