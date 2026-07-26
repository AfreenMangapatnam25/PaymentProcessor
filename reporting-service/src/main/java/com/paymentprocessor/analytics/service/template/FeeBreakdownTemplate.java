package com.paymentprocessor.analytics.service.template;

import com.paymentprocessor.analytics.domain.enums.ReportType;
import com.paymentprocessor.analytics.service.query.Aggregation;
import com.paymentprocessor.analytics.service.query.ClickHouseQueryExecutor;
import com.paymentprocessor.analytics.service.query.DatasetCatalog;
import com.paymentprocessor.analytics.service.query.QuerySpec;
import com.paymentprocessor.analytics.service.query.QuerySpec.MeasureRef;
import com.paymentprocessor.analytics.service.query.SafeQueryBuilder;
import com.paymentprocessor.analytics.service.report.ReportData;
import com.paymentprocessor.analytics.service.report.ReportSection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Fee breakdown by connector and card brand for a merchant. */
@Component
// NOTE: no @ConditionalOnBean("clickHouseJdbcTemplate") here.
// ClickHouse is optional (analytics.clickhouse.enabled defaults to false). Making this
// bean conditional removed it from the context, which broke unconditional consumers
// (ReportService / ReportGenerationWorker) and prevented the whole service from starting
// on Postgres alone. ClickHouseQueryExecutor now guards at call time instead, so an
// OLAP-backed request fails with a clear message rather than at boot.
public class FeeBreakdownTemplate extends AbstractQueryTemplate {

    public FeeBreakdownTemplate(DatasetCatalog c, SafeQueryBuilder b, ClickHouseQueryExecutor e) {
        super(c, b, e);
    }

    @Override
    public ReportType type() { return ReportType.FEE_BREAKDOWN; }

    @Override
    public ReportData generate(ReportParameters p) {
        List<MeasureRef> measures = List.of(
                new MeasureRef("amount_minor", Aggregation.SUM),
                new MeasureRef("fee_minor", Aggregation.SUM),
                new MeasureRef("interchange_minor", Aggregation.SUM),
                new MeasureRef(null, Aggregation.COUNT));

        QuerySpec byConnector = new QuerySpec("fees",
                List.of("connector", "card_brand", "currency"), measures,
                List.of(merchantFilter(p.merchantId())), p.fromDate(), p.toDate(),
                List.of(new QuerySpec.OrderBy("sum_fee_minor", true)), 2_000, 0);
        ReportSection section = sectionFrom("Fees by Connector & Card Brand", run(byConnector));

        return new ReportData(
                "Fee Breakdown",
                "Merchant " + p.merchantId() + " — " + p.fromDate() + " to " + p.toDate(),
                Map.of("merchant", p.merchantId(), "from", p.fromDate().toString(), "to", p.toDate().toString()),
                List.of(section));
    }
}
