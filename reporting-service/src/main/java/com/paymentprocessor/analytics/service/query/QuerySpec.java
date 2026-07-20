package com.paymentprocessor.analytics.service.query;

import java.time.LocalDate;
import java.util.List;

/**
 * Validated, immutable description of an ad-hoc query. Built from a request DTO after
 * every field has been checked against the {@link DatasetCatalog}.
 */
public record QuerySpec(
        String dataset,
        List<String> dimensions,
        List<MeasureRef> measures,
        List<Filter> filters,
        LocalDate fromDate,
        LocalDate toDate,
        List<OrderBy> orderBy,
        int limit,
        int offset) {

    public record MeasureRef(String column, Aggregation aggregation) { }
    public record Filter(String column, FilterOp op, List<Object> values) { }
    public record OrderBy(String alias, boolean descending) { }

    public boolean isAggregated() { return measures != null && !measures.isEmpty(); }
}
