package com.hfenelsoftllc.securitycommon.service;

import jakarta.servlet.http.HttpServletRequest;

public class BearerTokenService {

    public String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7).trim();
    }

    public String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Missing Authorization header");
        }

        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix) || authorizationHeader.length() <= prefix.length()) {
            throw new IllegalArgumentException("Authorization header must use ******");
        }

        return authorizationHeader.substring(prefix.length()).trim();
    }
}
