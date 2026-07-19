package com.paymentprocessor.analytics.dto;

import com.paymentprocessor.analytics.service.query.Aggregation;
import com.paymentprocessor.analytics.service.query.FilterOp;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Ad-hoc query builder request. Validated against the DatasetCatalog before execution. */
public record QueryRequest(
        @NotBlank String dataset,
        List<String> dimensions,
        List<Measure> measures,
        List<Filter> filters,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        List<Order> orderBy,
        Integer limit,
        Integer offset) {

    public record Measure(@NotBlank String column, @NotNull Aggregation aggregation) { }
    public record Filter(@NotBlank String column, @NotNull FilterOp op, List<Object> values) { }
    public record Order(@NotBlank String by, boolean desc) { }
}
