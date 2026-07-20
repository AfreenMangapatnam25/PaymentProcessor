package com.paymentprocessor.notificationservice;

import org.junit.jupiter.api.Test;

/**
 * The cheapest possible test with the highest signal: if the Spring context
 * (every controller/service/repository/config bean, Flyway migration, and
 * their constructor-injection graph) doesn't wire up cleanly against a real
 * Postgres, this fails fast without needing to reason about business logic.
 */
class NotificationServiceApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // Intentionally empty: AbstractIntegrationTest's @SpringBootTest does the work.
    }
}
