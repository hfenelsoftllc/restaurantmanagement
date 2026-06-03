package com.hfenelsoftllc.order.config;

import com.hfenelsoftllc.securitycommon.service.BearerTokenService;
import com.hfenelsoftllc.securitycommon.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Validates the JWT Bearer token on every request that Spring Security does not
 * already permit. Populates the SecurityContext with the authenticated user's ID.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final BearerTokenService bearerTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   BearerTokenService bearerTokenService) {
        this.jwtTokenService = jwtTokenService;
        this.bearerTokenService = bearerTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = bearerTokenService.extractToken(request);

        if (token != null && !token.isBlank()) {
            try {
                var claims = jwtTokenService.parseClaims(token);

                var auth = new UsernamePasswordAuthenticationToken(
                        claims.userId(), null, Collections.emptyList());
                auth.setDetails(claims);
                SecurityContextHolder.getContext().setAuthentication(auth);

                MDC.put("userId", String.valueOf(claims.userId()));
                MDC.put("email",  claims.email());

            } catch (Exception ex) {
                log.warn("JWT validation failed on [{}]: {}", request.getRequestURI(), ex.getMessage());
                // Do NOT populate SecurityContext — Spring Security will reject unauthenticated access
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/actuator")
               || path.startsWith("/swagger-ui")
               || path.startsWith("/v3/api-docs");
    }
}
