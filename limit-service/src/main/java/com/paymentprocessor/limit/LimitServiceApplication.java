package com.paymentprocessor.limit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Limit Service — enforces spending and transaction limits before a payment is
 * authorized. Acts as the platform financial guardrail: validates per-transaction,
 * daily, weekly, and monthly amount/count limits and manages reserve/commit/release
 * of limit capacity to prevent race conditions and double spending.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class LimitServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LimitServiceApplication.class, args);
    }
}
