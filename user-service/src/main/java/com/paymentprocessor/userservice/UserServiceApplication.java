package com.paymentprocessor.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * user-service entrypoint. {@code @ConfigurationPropertiesScan} binds the
 * {@code app.*} properties; {@code @EnableScheduling} drives the outbox relay.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
