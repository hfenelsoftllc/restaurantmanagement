package com.hfenelsoftllc.foodcatalogue.service;

import com.hfenelsoftllc.foodcatalogue.exception.AuthenticationFailedException;
import com.hfenelsoftllc.foodcatalogue.support.JwtTestTokenFactory;
import com.hfenelsoftllc.securitycommon.config.SharedJwtProperties;
import com.hfenelsoftllc.securitycommon.service.AuthValidationClient;
import com.hfenelsoftllc.securitycommon.service.BearerTokenService;
import com.hfenelsoftllc.securitycommon.service.JwtTokenService;
import com.hfenelsoftllc.securitycommon.service.TokenClaims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationServiceTest {

    private static final String JWT_SECRET = "test-jwt-secret-key-test-jwt-secret-key-123456";

    @Mock
    private AuthValidationClient authValidationClient;

    @Test
    void validateAccessTokenShouldReturnClaimsForValidBearerToken() {
        SharedJwtProperties properties = new SharedJwtProperties();
        properties.setSecret(JWT_SECRET);
        properties.setExpirationMinutes(60);

        JwtTokenService jwtTokenService = new JwtTokenService(properties, new com.fasterxml.jackson.databind.ObjectMapper());
        JwtAuthenticationService authenticationService = new JwtAuthenticationService(
                jwtTokenService,
                new BearerTokenService(),
                authValidationClient,
                true
        );
        String token = JwtTestTokenFactory.createToken(JWT_SECRET, 7L, "food@example.com", "session-v1");

        doNothing().when(authValidationClient).validateAuthorizationHeader("Bearer " + token);

        TokenClaims claims = authenticationService.validateAccessToken("Bearer " + token);

        assertEquals(7L, claims.userId());
        assertEquals("food@example.com", claims.email());
        assertEquals("session-v1", claims.sessionVersion());
        verify(authValidationClient).validateAuthorizationHeader("Bearer " + token);
    }

    @Test
    void validateAccessTokenShouldRejectMissingAuthorizationHeader() {
        SharedJwtProperties properties = new SharedJwtProperties();
        properties.setSecret(JWT_SECRET);
        properties.setExpirationMinutes(60);

        JwtTokenService jwtTokenService = new JwtTokenService(properties, new com.fasterxml.jackson.databind.ObjectMapper());
        JwtAuthenticationService authenticationService = new JwtAuthenticationService(
                jwtTokenService,
                new BearerTokenService(),
                authValidationClient,
                false
        );

        AuthenticationFailedException exception = assertThrows(
                AuthenticationFailedException.class,
                () -> authenticationService.validateAccessToken(null)
        );

        assertEquals("Invalid or expired token", exception.getMessage());
    }
}

