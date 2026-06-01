package com.hfenelsoftllc.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cloud.vault.enabled=false",
        "spring.cloud.config.server.vault.enabled=false"
})
class ConfigServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
