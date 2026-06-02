package com.hfenelsoftllc.securitycommon.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hfenelsoftllc.securitycommon.service.AuthValidationClient;
import com.hfenelsoftllc.securitycommon.service.BearerTokenService;
import com.hfenelsoftllc.securitycommon.service.JwtTokenService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({SharedJwtProperties.class, AuthValidationProperties.class})
public class SharedSecurityConfiguration {

    @Bean
    public BearerTokenService bearerTokenService() {
        return new BearerTokenService();
    }

    @Bean
    public JwtTokenService jwtTokenService(SharedJwtProperties jwtProperties, ObjectMapper objectMapper) {
        return new JwtTokenService(jwtProperties, objectMapper);
    }

    @Bean
    public RestClient.Builder authValidationRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public AuthValidationClient authValidationClient(
            @Qualifier("authValidationRestClientBuilder") RestClient.Builder restClientBuilder,
            AuthValidationProperties authValidationProperties
    ) {
        return new AuthValidationClient(restClientBuilder.build(), authValidationProperties);
    }
}

