package com.paymentprocessor.ledgerservice.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Core application beans. Scheduling is enabled here to drive the outbox relay.
 */
@Configuration
@EnableScheduling
public class AppConfig {

    /** A UTC clock, injected into services so time is deterministic and testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
