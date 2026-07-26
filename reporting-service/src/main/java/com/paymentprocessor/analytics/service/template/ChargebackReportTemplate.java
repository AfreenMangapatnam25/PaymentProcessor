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

/** Merchant chargeback/dispute report: summary by reason code + dispute detail. */
@Component
// NOTE: no @ConditionalOnBean("clickHouseJdbcTemplate") here.
// ClickHouse is optional (analytics.clickhouse.enabled defaults to false). Making this
// bean conditional removed it from the context, which broke unconditional consumers
// (ReportService / ReportGenerationWorker) and prevented the whole service from starting
// on Postgres alone. ClickHouseQueryExecutor now guards at call time instead, so an
// OLAP-backed request fails with a clear message rather than at boot.
public class ChargebackReportTemplate extends AbstractQueryTemplate {

    public ChargebackReportTemplate(DatasetCatalog c, SafeQueryBuilder b, ClickHouseQueryExecutor e) {
        super(c, b, e);
    }

    @Override
    public ReportType type() { return ReportType.CHARGEBACK_REPORT; }

    @Override
    public ReportData generate(ReportParameters p) {
        QuerySpec byReason = new QuerySpec("disputes",
                List.of("reason_code", "category"),
                List.of(new MeasureRef(null, Aggregation.COUNT),
                        new MeasureRef("amount_minor", Aggregation.SUM)),
                List.of(merchantFilter(p.merchantId())), p.fromDate(), p.toDate(),
                List.of(new QuerySpec.OrderBy("cnt", true)), 500, 0);
        ReportSection summary = sectionFrom("Chargebacks by Reason", run(byReason));

        QuerySpec detail = new QuerySpec("disputes",
                List.of("dispute_id", "intent_id", "opened_date", "reason_code",
                        "status", "outcome", "card_brand", "currency", "amount_minor"),
                List.of(),
                List.of(merchantFilter(p.merchantId())), p.fromDate(), p.toDate(),
                List.of(new QuerySpec.OrderBy("opened_date", true)), 50_000, 0);
        ReportSection detailSection = sectionFrom("Dispute Detail", run(detail));

        return new ReportData(
                "Chargeback Report",
                "Merchant " + p.merchantId() + " — " + p.fromDate() + " to " + p.toDate(),
                Map.of("merchant", p.merchantId(), "from", p.fromDate().toString(), "to", p.toDate().toString()),
                List.of(summary, detailSection));
    }
}
