package com.paymentprocessor.merchantservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Merchant Service — system of record for merchant identity, onboarding, KYB/KYC compliance
 * status, financial configuration, integration credentials, and merchant lifecycle. Publishes
 * domain events for downstream services via a transactional outbox relayed to Kafka.
 */
@SpringBootApplication
@EnableScheduling
public class MerchantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MerchantServiceApplication.class, args);
    }
}
