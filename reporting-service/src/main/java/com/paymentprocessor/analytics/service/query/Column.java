package com.paymentprocessor.analytics.service.query;

/**
 * A whitelisted, queryable column. {@code measure=true} means it may be aggregated
 * (SUM/AVG/…); dimensions may be grouped and filtered on.
 * {@code physical} is the real ClickHouse column expression — never user input.
 */
public record Column(String name, String physical, ColumnType type, boolean measure, String label) {
    public static Column dimension(String name, String physical, ColumnType type, String label) {
        return new Column(name, physical, type, false, label);
    }
    public static Column measure(String name, String physical, ColumnType type, String label) {
        return new Column(name, physical, type, true, label);
    }
}
