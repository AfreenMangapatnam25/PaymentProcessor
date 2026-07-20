package com.paymentprocessor.analytics.service.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Executes {@link SafeQuery} statements against the read-only ClickHouse replica.
 * Offers both a materialized result (bounded) and a streaming row callback so large
 * exports never hold the whole result set in memory.
 */
@Component
public class ClickHouseQueryExecutor {

    private final JdbcTemplate clickHouse;

    public ClickHouseQueryExecutor(@Qualifier("clickHouseJdbcTemplate") JdbcTemplate clickHouse) {
        this.clickHouse = clickHouse;
    }

    public QueryResult execute(SafeQuery q) {
        List<List<Object>> rows = clickHouse.query(q.sql(), q.params().toArray(), (rs, n) -> readRow(rs, q));
        return new QueryResult(q.outputColumns(), rows);
    }

    /** Streams rows one at a time to {@code consumer}; returns the row count processed. */
    public long stream(SafeQuery q, Consumer<List<Object>> consumer) {
        long[] count = {0};
        clickHouse.query(q.sql(), q.params().toArray(), rs -> {
            consumer.accept(readRow(rs, q));
            count[0]++;
        });
        return count[0];
    }

    private List<Object> readRow(ResultSet rs, SafeQuery q) throws SQLException {
        List<Object> row = new ArrayList<>(q.outputColumns().size());
        for (int i = 0; i < q.outputColumns().size(); i++) {
            row.add(rs.getObject(i + 1));
        }
        return row;
    }
}
