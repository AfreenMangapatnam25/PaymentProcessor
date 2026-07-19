package com.paymentprocessor.analytics.service.template;

import com.paymentprocessor.analytics.service.query.ClickHouseQueryExecutor;
import com.paymentprocessor.analytics.service.query.ColumnType;
import com.paymentprocessor.analytics.service.query.DatasetCatalog;
import com.paymentprocessor.analytics.service.query.QueryColumn;
import com.paymentprocessor.analytics.service.query.QueryResult;
import com.paymentprocessor.analytics.service.query.QuerySpec;
import com.paymentprocessor.analytics.service.query.SafeQueryBuilder;
import com.paymentprocessor.analytics.service.report.ReportSection;
import java.util.List;

/**
 * Shared plumbing for templates: builds trusted {@link QuerySpec}s and runs them through
 * the same safe builder + read-only executor used by the ad-hoc query path.
 */
public abstract class AbstractQueryTemplate implements ReportTemplate {

    protected final DatasetCatalog catalog;
    protected final SafeQueryBuilder builder;
    protected final ClickHouseQueryExecutor executor;

    protected AbstractQueryTemplate(DatasetCatalog catalog, SafeQueryBuilder builder,
                                    ClickHouseQueryExecutor executor) {
        this.catalog = catalog;
        this.builder = builder;
        this.executor = executor;
    }

    protected QueryResult run(QuerySpec spec) {
        return executor.execute(builder.build(spec, catalog.get(spec.dataset())));
    }

    protected ReportSection sectionFrom(String title, QueryResult result) {
        List<String> headers = result.columns().stream().map(QueryColumn::label).toList();
        List<ColumnType> types = result.columns().stream().map(QueryColumn::type).toList();
        return new ReportSection(title, headers, types, result.rows());
    }

    /** Convenience for the mandatory merchant-id equality filter. */
    protected QuerySpec.Filter merchantFilter(String merchantId) {
        return new QuerySpec.Filter("merchant_id",
                com.paymentprocessor.analytics.service.query.FilterOp.EQ, List.of(merchantId));
    }
}
