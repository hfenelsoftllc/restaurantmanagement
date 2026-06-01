package com.hfenelsoftllc.restaurantlisting.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI restaurantListingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Restaurant Listing API")
                        .description("Restaurant management and restaurant details aggregation endpoints")
                        .version("v1")
                        .contact(new Contact().name("Restaurant Management Team"))
                        .license(new License().name("Internal Use")));
    }
}

