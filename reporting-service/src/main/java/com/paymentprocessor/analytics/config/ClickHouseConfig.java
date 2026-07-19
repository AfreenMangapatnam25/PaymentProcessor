package com.paymentprocessor.analytics.config;

import com.clickhouse.jdbc.ClickHouseDataSource;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Read-only JDBC access to the ClickHouse OLAP replica.
 * Deliberately separate from the primary (Postgres) DataSource: reports must never
 * touch OLTP, and this connection is scoped to a read-only ClickHouse role.
 */
@Configuration
public class ClickHouseConfig {

    @Bean(name = "clickHouseDataSource")
    public DataSource clickHouseDataSource(AnalyticsProperties props) throws SQLException {
        AnalyticsProperties.ClickHouse ch = props.getClickhouse();
        Properties p = new Properties();
        p.setProperty("user", ch.getUsername());
        p.setProperty("password", ch.getPassword());
        p.setProperty("socket_timeout", String.valueOf(ch.getQueryTimeoutSeconds() * 1000L));
        p.setProperty("max_execution_time", String.valueOf(ch.getQueryTimeoutSeconds()));
        // Enforce read-only at the protocol level as defense-in-depth.
        p.setProperty("readonly", "2");
        p.setProperty("max_result_rows", String.valueOf(ch.getMaxResultRows()));
        return new ClickHouseDataSource(ch.getUrl(), p);
    }

    @Bean(name = "clickHouseJdbcTemplate")
    public JdbcTemplate clickHouseJdbcTemplate(
            @org.springframework.beans.factory.annotation.Qualifier("clickHouseDataSource") DataSource ds,
            AnalyticsProperties props) {
        JdbcTemplate t = new JdbcTemplate(ds);
        t.setQueryTimeout(props.getClickhouse().getQueryTimeoutSeconds());
        t.setMaxRows((int) Math.min(Integer.MAX_VALUE, props.getClickhouse().getMaxResultRows()));
        return t;
    }
}
