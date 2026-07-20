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

/** Merchant settlement summary: totals block + per-day breakdown. */
@Component
public class SettlementSummaryTemplate extends AbstractQueryTemplate {

    public SettlementSummaryTemplate(DatasetCatalog c, SafeQueryBuilder b, ClickHouseQueryExecutor e) {
        super(c, b, e);
    }

    @Override
    public ReportType type() { return ReportType.SETTLEMENT_SUMMARY; }

    @Override
    public ReportData generate(ReportParameters p) {
        List<MeasureRef> measures = List.of(
                new MeasureRef("gross_minor", Aggregation.SUM),
                new MeasureRef("fee_minor", Aggregation.SUM),
                new MeasureRef("refund_minor", Aggregation.SUM),
                new MeasureRef("chargeback_minor", Aggregation.SUM),
                new MeasureRef("net_minor", Aggregation.SUM),
                new MeasureRef("txn_count", Aggregation.SUM));

        // Totals across the whole window.
        QuerySpec totals = new QuerySpec("settlements", List.of("currency"), measures,
                List.of(merchantFilter(p.merchantId())), p.fromDate(), p.toDate(),
                List.of(), 100, 0);
        ReportSection totalsSection = sectionFrom("Totals by Currency", run(totals));

        // Daily breakdown.
        QuerySpec daily = new QuerySpec("settlements", List.of("settlement_date", "currency"), measures,
                List.of(merchantFilter(p.merchantId())), p.fromDate(), p.toDate(),
                List.of(new QuerySpec.OrderBy("settlement_date", false)), 10_000, 0);
        ReportSection dailySection = sectionFrom("Daily Settlements", run(daily));

        return new ReportData(
                "Settlement Summary",
                "Merchant " + p.merchantId() + " — " + p.fromDate() + " to " + p.toDate(),
                Map.of("merchant", p.merchantId(), "from", p.fromDate().toString(), "to", p.toDate().toString()),
                List.of(totalsSection, dailySection));
    }
}
