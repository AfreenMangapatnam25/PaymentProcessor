package com.paymentprocessor.auditservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * audit-service — the immutable, tamper-evident audit trail.
 *
 * <p>PostgreSQL holds the query copy; the legal copy is a daily signed batch written to
 * S3 with Object Lock (WORM). Every record is hash-chained to its predecessor so any
 * tampering is detectable, and the daily root hash is anchored externally.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
