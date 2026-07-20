package com.paymentprocessor.analytics.service.template;

import com.paymentprocessor.analytics.domain.enums.ReportType;
import com.paymentprocessor.analytics.service.query.ClickHouseQueryExecutor;
import com.paymentprocessor.analytics.service.query.DatasetCatalog;
import com.paymentprocessor.analytics.service.query.QuerySpec;
import com.paymentprocessor.analytics.service.query.SafeQueryBuilder;
import com.paymentprocessor.analytics.service.report.ReportData;
import com.paymentprocessor.analytics.service.report.ReportSection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Flat transaction detail export for a merchant over a date range. */
@Component
public class TransactionDetailTemplate extends AbstractQueryTemplate {

    public TransactionDetailTemplate(DatasetCatalog c, SafeQueryBuilder b, ClickHouseQueryExecutor e) {
        super(c, b, e);
    }

    @Override
    public ReportType type() { return ReportType.TRANSACTION_DETAIL; }

    @Override
    public ReportData generate(ReportParameters p) {
        QuerySpec detail = new QuerySpec("transactions",
                List.of("intent_id", "created_at", "status", "currency", "amount_minor",
                        "fee_minor", "connector", "card_brand", "issuer_country", "decline_code"),
                List.of(),
                List.of(merchantFilter(p.merchantId())), p.fromDate(), p.toDate(),
                List.of(new QuerySpec.OrderBy("created_at", true)), 100_000, 0);
        ReportSection section = sectionFrom("Transactions", run(detail));

        return new ReportData(
                "Transaction Detail",
                "Merchant " + p.merchantId() + " — " + p.fromDate() + " to " + p.toDate(),
                Map.of("merchant", p.merchantId(), "from", p.fromDate().toString(), "to", p.toDate().toString()),
                List.of(section));
    }
}
