package com.paymentprocessor.analytics.service.query;

import java.util.Set;

public enum FilterOp {
    EQ("="), NE("!="), GT(">"), GTE(">="), LT("<"), LTE("<="),
    IN("IN"), NOT_IN("NOT IN"), LIKE("LIKE"), BETWEEN("BETWEEN");

    private final String sql;
    FilterOp(String sql) { this.sql = sql; }
    public String sql() { return sql; }

    public boolean isMultiValue() { return this == IN || this == NOT_IN; }
    public boolean isRange() { return this == BETWEEN; }

    private static final Set<FilterOp> STRING_OPS = Set.of(EQ, NE, IN, NOT_IN, LIKE);
    public boolean allowedForType(ColumnType type) {
        if (type == ColumnType.STRING || type == ColumnType.BOOLEAN) {
            return STRING_OPS.contains(this) || this == EQ || this == NE;
        }
        return this != LIKE; // numbers/dates support everything except LIKE
    }
}
