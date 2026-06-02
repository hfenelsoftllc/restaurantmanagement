package com.hfenelsoftllc.foodcatalogue.service;

import com.hfenelsoftllc.foodcatalogue.exception.AuthenticationFailedException;
import com.hfenelsoftllc.securitycommon.service.AuthValidationClient;
import com.hfenelsoftllc.securitycommon.service.BearerTokenService;
import com.hfenelsoftllc.securitycommon.service.JwtTokenService;
import com.hfenelsoftllc.securitycommon.service.TokenClaims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtAuthenticationService {
    private final JwtTokenService jwtTokenService;
    private final BearerTokenService bearerTokenService;
    private final AuthValidationClient authValidationClient;
    private final boolean authServiceValidationEnabled;

    public JwtAuthenticationService(
            JwtTokenService jwtTokenService,
            BearerTokenService bearerTokenService,
            AuthValidationClient authValidationClient,
            @Value("${integration.auth-service.enabled:true}") boolean authServiceValidationEnabled
    ) {
        this.jwtTokenService = jwtTokenService;
        this.bearerTokenService = bearerTokenService;
        this.authValidationClient = authValidationClient;
        this.authServiceValidationEnabled = authServiceValidationEnabled;
    }

    public TokenClaims validateAccessToken(String authorizationHeader) {
        try {
            String bearerToken = bearerTokenService.extractBearerToken(authorizationHeader);
            TokenClaims claims = jwtTokenService.parseClaims(bearerToken);
            if (authServiceValidationEnabled) {
                authValidationClient.validateAuthorizationHeader(authorizationHeader);
            }
            return claims;
        } catch (IllegalArgumentException ex) {
            throw new AuthenticationFailedException("Invalid or expired token");
        }
    }
}

