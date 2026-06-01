package com.hfenelsoftllc.usermanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userManagementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Management API")
                        .description("User registration and retrieval endpoints")
                        .version("v1")
                        .contact(new Contact().name("Restaurant Management Team"))
                        .license(new License().name("Internal Use")));
    }
}

