package com.hfenelsoftllc.restaurantlisting;

import com.hfenelsoftllc.securitycommon.config.SharedSecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(SharedSecurityConfiguration.class)
public class RestaurantlistingApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantlistingApplication.class, args);
    }

}
