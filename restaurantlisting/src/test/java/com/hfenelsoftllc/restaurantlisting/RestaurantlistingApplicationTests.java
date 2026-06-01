package com.hfenelsoftllc.restaurantlisting;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "spring.main.lazy-initialization=true",
        "eureka.client.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false"
})
class RestaurantlistingApplicationTests {

    @Test
    void contextLoads() {
    }

}
