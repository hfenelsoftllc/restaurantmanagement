package com.hfenelsoftllc.securitycommon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.auth-service")
public class AuthValidationProperties {
    private String baseUrl = "http://localhost:9090";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}

