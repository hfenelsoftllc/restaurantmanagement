package com.hfenelsoftllc.payment;

import com.hfenelsoftllc.securitycommon.config.SharedSecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(SharedSecurityConfiguration.class)
public class PaymentServiceInternalApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceInternalApplication.class, args);
    }
}

