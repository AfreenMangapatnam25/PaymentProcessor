package com.paymentprocessor.analytics.service.query;

import java.util.List;

/** Materialized ad-hoc query result. */
public record QueryResult(List<QueryColumn> columns, List<List<Object>> rows) {
    public int rowCount() { return rows.size(); }
}
