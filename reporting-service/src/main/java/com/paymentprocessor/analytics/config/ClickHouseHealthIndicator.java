package com.paymentprocessor.analytics.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Surfaces ClickHouse reachability under /actuator/health. */
@Component("clickHouse")
@ConditionalOnBean(name = "clickHouseJdbcTemplate")
public class ClickHouseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate clickHouse;

    public ClickHouseHealthIndicator(@Qualifier("clickHouseJdbcTemplate") JdbcTemplate clickHouse) {
        this.clickHouse = clickHouse;
    }

    @Override
    public Health health() {
        try {
            Integer one = clickHouse.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1
                    ? Health.up().withDetail("store", "clickhouse").build()
                    : Health.down().withDetail("reason", "unexpected ping result").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("store", "clickhouse").build();
        }
    }
}
