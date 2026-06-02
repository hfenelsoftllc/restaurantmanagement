package com.hfenelsoftllc.securitycommon.service;

import jakarta.servlet.http.HttpServletRequest;

public class BearerTokenService {

    public String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        if (!authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7).trim();
    }
}

