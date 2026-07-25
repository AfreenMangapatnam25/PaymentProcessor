package com.paymentprocessor.analytics.service.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Executes {@link SafeQuery} statements against the read-only ClickHouse replica.
 * Offers both a materialized result (bounded) and a streaming row callback so large
 * exports never hold the whole result set in memory.
 *
 * <h2>Running without ClickHouse</h2>
 * ClickHouse is optional: {@code analytics.clickhouse.enabled} defaults to {@code false}
 * (env {@code CLICKHOUSE_ENABLED}), so a local/dev deployment can run on Postgres alone.
 *
 * <p>This class is therefore <b>always registered</b>, and holds its {@link JdbcTemplate}
 * through an {@link ObjectProvider} that resolves to nothing when ClickHouse is disabled.
 * The previous approach — annotating this and its dependents with
 * {@code @ConditionalOnBean(name = "clickHouseJdbcTemplate")} — removed the bean entirely,
 * which broke startup: {@code ReportService} and {@code ReportGenerationWorker} are
 * unconditional {@code @Service}/{@code @Component} beans that require
 * {@link QueryBuilderService}, so the whole context failed to build with ClickHouse off.
 * ({@code @ConditionalOnBean} is documented as being for auto-configuration classes only;
 * on ordinary components its evaluation order is not guaranteed.)
 *
 * <p>The trade-off is deliberate: an OLAP-backed operation now fails when it is
 * <i>invoked</i>, with an actionable message, rather than preventing the service from
 * starting at all. Everything backed by Postgres — report job records, schedules, status —
 * keeps working.
 */
@Component
public class ClickHouseQueryExecutor {

    /** Bean name published by {@code ClickHouseConfig} when ClickHouse is enabled. */
    private static final String CLICKHOUSE_TEMPLATE_BEAN = "clickHouseJdbcTemplate";

    /**
     * Resolved by <b>bean name</b> rather than by type. This service also has a Postgres
     * {@code JdbcTemplate} (from {@code spring-boot-starter-jdbc}), so a by-type lookup would
     * be ambiguous once ClickHouse is switched on — and, worse, could silently hand OLAP
     * queries to the OLTP database.
     */
    private final ApplicationContext context;

    /**
     * @param context used to look up the ClickHouse template by name, only when a query is
     *                actually run
     */
    public ClickHouseQueryExecutor(ApplicationContext context) {
        this.context = context;
    }

    /**
     * @return true when a ClickHouse connection is configured and this executor can run
     *         queries. Callers that can degrade gracefully should check this first.
     */
    public boolean isAvailable() {
        return context.containsBean(CLICKHOUSE_TEMPLATE_BEAN);
    }

    /**
     * Runs a query and materialises the (bounded) result set.
     *
     * @param q the validated, parameterised query to run
     * @return the column metadata plus all result rows
     * @throws ClickHouseUnavailableException if ClickHouse is not configured
     */
    public QueryResult execute(SafeQuery q) {
        JdbcTemplate clickHouse = require();
        List<List<Object>> rows = clickHouse.query(q.sql(), q.params().toArray(), (rs, n) -> readRow(rs, q));
        return new QueryResult(q.outputColumns(), rows);
    }

    /**
     * Streams rows one at a time to {@code consumer}; returns the row count processed.
     *
     * @param q        the validated, parameterised query to run
     * @param consumer receives each row as it is read
     * @return how many rows were streamed
     * @throws ClickHouseUnavailableException if ClickHouse is not configured
     */
    public long stream(SafeQuery q, Consumer<List<Object>> consumer) {
        JdbcTemplate clickHouse = require();
        long[] count = {0};
        clickHouse.query(q.sql(), q.params().toArray(), rs -> {
            consumer.accept(readRow(rs, q));
            count[0]++;
        });
        return count[0];
    }

    /**
     * Returns the ClickHouse template, or fails with an explanation of exactly what to
     * configure.
     *
     * @return the ClickHouse {@link JdbcTemplate}
     * @throws ClickHouseUnavailableException when ClickHouse is disabled
     */
    private JdbcTemplate require() {
        if (!context.containsBean(CLICKHOUSE_TEMPLATE_BEAN)) {
            throw new ClickHouseUnavailableException(
                    "This operation reads from the ClickHouse OLAP replica, which is not "
                    + "configured. Set analytics.clickhouse.enabled=true (env CLICKHOUSE_ENABLED) "
                    + "and point analytics.clickhouse.url (env CLICKHOUSE_URL) at a reachable "
                    + "ClickHouse instance. Report-job and schedule endpoints, which are backed "
                    + "by Postgres, work without it.");
        }
        return context.getBean(CLICKHOUSE_TEMPLATE_BEAN, JdbcTemplate.class);
    }

    /**
     * Reads the current row into a positional value list matching the query's output columns.
     *
     * @param rs the JDBC result set positioned on a row
     * @param q  the query whose output columns describe the row shape
     * @return the row values in column order
     * @throws SQLException if a column cannot be read
     */
    private List<Object> readRow(ResultSet rs, SafeQuery q) throws SQLException {
        List<Object> row = new ArrayList<>(q.outputColumns().size());
        for (int i = 0; i < q.outputColumns().size(); i++) {
            row.add(rs.getObject(i + 1));
        }
        return row;
    }

    /**
     * Thrown when an OLAP-backed operation is attempted while ClickHouse is disabled.
     * Distinct from a connectivity failure: this means the feature was never switched on.
     */
    public static class ClickHouseUnavailableException extends IllegalStateException {
        /**
         * @param message actionable description of what to configure
         */
        public ClickHouseUnavailableException(String message) {
            super(message);
        }
    }
}
