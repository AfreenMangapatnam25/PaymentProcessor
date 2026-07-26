package com.paymentprocessor.analytics.service.query;

import com.paymentprocessor.analytics.dto.QueryRequest;
import com.paymentprocessor.analytics.exception.QueryValidationException;
import com.paymentprocessor.analytics.service.report.ReportData;
import com.paymentprocessor.analytics.service.report.ReportSection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Turns an untrusted {@link QueryRequest} into a validated {@link QuerySpec}, then into
 * an executable {@link SafeQuery}. All identifier validation happens here and in
 * {@link SafeQueryBuilder}; nothing downstream trusts raw user input.
 */
@Service
// NOTE: no @ConditionalOnBean("clickHouseJdbcTemplate") here.
// ClickHouse is optional (analytics.clickhouse.enabled defaults to false). Making this
// bean conditional removed it from the context, which broke unconditional consumers
// (ReportService / ReportGenerationWorker) and prevented the whole service from starting
// on Postgres alone. ClickHouseQueryExecutor now guards at call time instead, so an
// OLAP-backed request fails with a clear message rather than at boot.
public class QueryBuilderService {

    private final DatasetCatalog catalog;
    private final SafeQueryBuilder builder;
    private final ClickHouseQueryExecutor executor;

    public QueryBuilderService(DatasetCatalog catalog, SafeQueryBuilder builder,
                               ClickHouseQueryExecutor executor) {
        this.catalog = catalog;
        this.builder = builder;
        this.executor = executor;
    }

    public QuerySpec validate(QueryRequest req) {
        if (req.dataset() == null || !catalog.exists(req.dataset())) {
            throw new QueryValidationException("Unknown dataset: " + req.dataset());
        }
        Dataset ds = catalog.get(req.dataset());

        List<String> dims = req.dimensions() == null ? List.of() : req.dimensions();
        for (String d : dims) {
            if (!ds.has(d)) throw new QueryValidationException("Unknown dimension: " + d);
        }

        List<QuerySpec.MeasureRef> measures = new ArrayList<>();
        if (req.measures() != null) {
            for (QueryRequest.Measure m : req.measures()) {
                if (m.aggregation() != Aggregation.COUNT && !ds.has(m.column())) {
                    throw new QueryValidationException("Unknown measure column: " + m.column());
                }
                measures.add(new QuerySpec.MeasureRef(m.column(), m.aggregation()));
            }
        }
        if (dims.isEmpty() && measures.isEmpty()) {
            throw new QueryValidationException("Provide at least one dimension or measure");
        }

        List<QuerySpec.Filter> filters = new ArrayList<>();
        if (req.filters() != null) {
            for (QueryRequest.Filter f : req.filters()) {
                if (!ds.has(f.column())) throw new QueryValidationException("Unknown filter column: " + f.column());
                List<Object> vals = f.values() == null ? List.of() : f.values();
                filters.add(new QuerySpec.Filter(f.column(), f.op(), vals));
            }
        }

        if (req.fromDate() == null || req.toDate() == null) {
            throw new QueryValidationException("fromDate and toDate are required");
        }
        if (req.toDate().isBefore(req.fromDate())) {
            throw new QueryValidationException("toDate must not be before fromDate");
        }

        List<QuerySpec.OrderBy> orders = new ArrayList<>();
        if (req.orderBy() != null) {
            for (QueryRequest.Order o : req.orderBy()) {
                orders.add(new QuerySpec.OrderBy(o.by(), o.desc()));
            }
        }

        int limit = req.limit() == null ? 1000 : req.limit();
        int offset = req.offset() == null ? 0 : req.offset();
        return new QuerySpec(ds.name(), dims, measures, filters,
                req.fromDate(), req.toDate(), orders, limit, offset);
    }

    public SafeQuery compile(QuerySpec spec) {
        return builder.build(spec, catalog.get(spec.dataset()));
    }

    /** Validate, compile, execute, and return a preview result (used by the API). */
    public QueryResult run(QueryRequest req) {
        return executor.execute(compile(validate(req)));
    }

    /** Render a query result as report data for export. */
    public ReportData toReportData(String title, QueryResult result) {
        List<String> headers = result.columns().stream().map(QueryColumn::label).toList();
        List<com.paymentprocessor.analytics.service.query.ColumnType> types =
                result.columns().stream().map(QueryColumn::type).toList();
        ReportSection section = new ReportSection("Results", headers, types, result.rows());
        return new ReportData(title, null, Map.of("rows", String.valueOf(result.rowCount())),
                List.of(section));
    }

    public DatasetCatalog catalog() { return catalog; }
    public ClickHouseQueryExecutor executor() { return executor; }
}
