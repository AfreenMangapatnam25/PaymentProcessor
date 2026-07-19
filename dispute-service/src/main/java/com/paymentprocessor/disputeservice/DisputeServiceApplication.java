package com.paymentprocessor.disputeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Dispute Service.
 *
 * <p>The Dispute Service manages the full lifecycle of payment disputes,
 * chargebacks, retrieval requests, representments and arbitration between
 * merchants, card networks and issuing banks. Scheduling is enabled so the
 * deadline monitor can proactively escalate or auto-close disputes.
 */
@SpringBootApplication
@EnableScheduling
public class DisputeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisputeServiceApplication.class, args);
    }
}
