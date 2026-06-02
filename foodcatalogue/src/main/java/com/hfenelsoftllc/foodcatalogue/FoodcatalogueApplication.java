package com.hfenelsoftllc.foodcatalogue;

import com.hfenelsoftllc.securitycommon.config.SharedSecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(SharedSecurityConfiguration.class)
public class FoodcatalogueApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodcatalogueApplication.class, args);
    }

}
