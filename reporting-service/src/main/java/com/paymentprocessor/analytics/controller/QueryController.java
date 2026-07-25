package com.paymentprocessor.analytics.controller;

import com.paymentprocessor.analytics.dto.QueryRequest;
import com.paymentprocessor.analytics.service.query.Column;
import com.paymentprocessor.analytics.service.query.Dataset;
import com.paymentprocessor.analytics.service.query.DatasetCatalog;
import com.paymentprocessor.analytics.service.query.QueryBuilderService;
import com.paymentprocessor.analytics.service.query.QueryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/query")
@Tag(name = "Query Builder", description = "Ad-hoc, catalog-safe queries over the OLAP replica")
// NOTE: no @ConditionalOnBean("clickHouseJdbcTemplate") here.
// ClickHouse is optional (analytics.clickhouse.enabled defaults to false). Making this
// bean conditional removed it from the context, which broke unconditional consumers
// (ReportService / ReportGenerationWorker) and prevented the whole service from starting
// on Postgres alone. ClickHouseQueryExecutor now guards at call time instead, so an
// OLAP-backed request fails with a clear message rather than at boot.
public class QueryController {

    private final QueryBuilderService queryBuilder;
    private final DatasetCatalog catalog;

    public QueryController(QueryBuilderService queryBuilder, DatasetCatalog catalog) {
        this.queryBuilder = queryBuilder;
        this.catalog = catalog;
    }

    @GetMapping("/datasets")
    @Operation(summary = "List queryable datasets and their columns")
    public Map<String, Object> datasets() {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        for (Dataset ds : catalog.all().values()) {
            List<Map<String, Object>> cols = ds.columns().values().stream()
                    .map(this::describe).toList();
            out.put(ds.name(), Map.of("dateColumn", ds.dateColumn(), "columns", cols));
        }
        return out;
    }

    @PostMapping("/run")
    @Operation(summary = "Validate and execute an ad-hoc query, returning a bounded preview")
    public QueryResult run(@Valid @RequestBody QueryRequest req) {
        return queryBuilder.run(req);
    }

    private Map<String, Object> describe(Column c) {
        return Map.of("name", c.name(), "label", c.label(),
                "type", c.type().name(), "measure", c.measure());
    }
}
