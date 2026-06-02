package com.hfenelsoftllc.securitycommon.service;

public record TokenClaims(Long userId, String email, String sessionVersion) {
}

