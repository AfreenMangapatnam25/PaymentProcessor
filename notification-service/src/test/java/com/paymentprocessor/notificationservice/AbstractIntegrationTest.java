package com.paymentprocessor.notificationservice;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base for tests that need a real Postgres (partitioning, FOR UPDATE SKIP
 * LOCKED, advisory locks, and native queries throughout this service don't
 * have an H2-compatible equivalent, so these run against the real thing via
 * Testcontainers rather than an in-memory substitute).
 *
 * The dispatcher's background poll is pushed out to once a day so it never
 * fires mid-test; tests that exercise the dispatcher call
 * WebhookDispatcher#dispatchBatch() directly instead.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notification_service_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("notification.dispatcher.poll-interval", () -> "24h");
    }
}
