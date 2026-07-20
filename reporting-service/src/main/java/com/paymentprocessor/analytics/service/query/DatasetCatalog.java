package com.paymentprocessor.analytics.service.query;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The single source of truth for what can be queried. Every identifier that ends up
 * in generated SQL originates here — user input is only ever matched against these
 * entries, never interpolated. This is the backbone of SQL-injection safety.
 */
@Component
public class DatasetCatalog {

    private final Map<String, Dataset> datasets = new LinkedHashMap<>();

    @PostConstruct
    void init() {
        // ---- transactions (fact_payments) ----
        Dataset tx = new Dataset("transactions", "fact_payments", "created_date")
                .add(Column.dimension("intent_id", "intent_id", ColumnType.STRING, "Payment ID"))
                .add(Column.dimension("merchant_id", "merchant_id", ColumnType.STRING, "Merchant"))
                .add(Column.dimension("created_date", "created_date", ColumnType.DATE, "Date"))
                .add(Column.dimension("created_at", "created_at", ColumnType.DATETIME, "Timestamp"))
                .add(Column.dimension("currency", "currency", ColumnType.STRING, "Currency"))
                .add(Column.dimension("status", "status", ColumnType.STRING, "Status"))
                .add(Column.dimension("connector", "connector", ColumnType.STRING, "Connector"))
                .add(Column.dimension("card_brand", "card_brand", ColumnType.STRING, "Card Brand"))
                .add(Column.dimension("issuer_country", "issuer_country", ColumnType.STRING, "Issuer Country"))
                .add(Column.dimension("is_3ds", "is_3ds", ColumnType.BOOLEAN, "3DS"))
                .add(Column.dimension("decline_code", "decline_code", ColumnType.STRING, "Decline Code"))
                .add(Column.measure("amount_minor", "amount_minor", ColumnType.NUMBER, "Amount (minor)"))
                .add(Column.measure("fee_minor", "fee_minor", ColumnType.NUMBER, "Fee (minor)"))
                .add(Column.measure("interchange_minor", "interchange_minor", ColumnType.NUMBER, "Interchange (minor)"))
                .add(Column.measure("risk_score", "risk_score", ColumnType.NUMBER, "Risk Score"));
        datasets.put(tx.name(), tx);

        // ---- settlements (fact_settlements) ----
        Dataset st = new Dataset("settlements", "fact_settlements", "settlement_date")
                .add(Column.dimension("settlement_id", "settlement_id", ColumnType.STRING, "Settlement ID"))
                .add(Column.dimension("merchant_id", "merchant_id", ColumnType.STRING, "Merchant"))
                .add(Column.dimension("settlement_date", "settlement_date", ColumnType.DATE, "Settlement Date"))
                .add(Column.dimension("currency", "currency", ColumnType.STRING, "Currency"))
                .add(Column.dimension("status", "status", ColumnType.STRING, "Status"))
                .add(Column.dimension("payout_id", "payout_id", ColumnType.STRING, "Payout ID"))
                .add(Column.measure("gross_minor", "gross_minor", ColumnType.NUMBER, "Gross (minor)"))
                .add(Column.measure("fee_minor", "fee_minor", ColumnType.NUMBER, "Fees (minor)"))
                .add(Column.measure("refund_minor", "refund_minor", ColumnType.NUMBER, "Refunds (minor)"))
                .add(Column.measure("chargeback_minor", "chargeback_minor", ColumnType.NUMBER, "Chargebacks (minor)"))
                .add(Column.measure("adjustment_minor", "adjustment_minor", ColumnType.NUMBER, "Adjustments (minor)"))
                .add(Column.measure("net_minor", "net_minor", ColumnType.NUMBER, "Net (minor)"))
                .add(Column.measure("txn_count", "txn_count", ColumnType.NUMBER, "Txn Count"));
        datasets.put(st.name(), st);

        // ---- fees (fact_payments — fee-focused projection) ----
        Dataset fees = new Dataset("fees", "fact_payments", "created_date")
                .add(Column.dimension("merchant_id", "merchant_id", ColumnType.STRING, "Merchant"))
                .add(Column.dimension("created_date", "created_date", ColumnType.DATE, "Date"))
                .add(Column.dimension("currency", "currency", ColumnType.STRING, "Currency"))
                .add(Column.dimension("connector", "connector", ColumnType.STRING, "Connector"))
                .add(Column.dimension("card_brand", "card_brand", ColumnType.STRING, "Card Brand"))
                .add(Column.measure("fee_minor", "fee_minor", ColumnType.NUMBER, "Processing Fee (minor)"))
                .add(Column.measure("interchange_minor", "interchange_minor", ColumnType.NUMBER, "Interchange (minor)"))
                .add(Column.measure("amount_minor", "amount_minor", ColumnType.NUMBER, "Volume (minor)"));
        datasets.put(fees.name(), fees);

        // ---- disputes (fact_disputes) ----
        Dataset dp = new Dataset("disputes", "fact_disputes", "opened_date")
                .add(Column.dimension("dispute_id", "dispute_id", ColumnType.STRING, "Dispute ID"))
                .add(Column.dimension("merchant_id", "merchant_id", ColumnType.STRING, "Merchant"))
                .add(Column.dimension("intent_id", "intent_id", ColumnType.STRING, "Payment ID"))
                .add(Column.dimension("opened_date", "opened_date", ColumnType.DATE, "Opened"))
                .add(Column.dimension("reason_code", "reason_code", ColumnType.STRING, "Reason Code"))
                .add(Column.dimension("category", "category", ColumnType.STRING, "Category"))
                .add(Column.dimension("status", "status", ColumnType.STRING, "Status"))
                .add(Column.dimension("outcome", "outcome", ColumnType.STRING, "Outcome"))
                .add(Column.dimension("card_brand", "card_brand", ColumnType.STRING, "Card Brand"))
                .add(Column.dimension("currency", "currency", ColumnType.STRING, "Currency"))
                .add(Column.measure("amount_minor", "amount_minor", ColumnType.NUMBER, "Amount (minor)"));
        datasets.put(dp.name(), dp);
    }

    public Dataset get(String name) { return datasets.get(name); }
    public boolean exists(String name) { return datasets.containsKey(name); }
    public Map<String, Dataset> all() { return Collections.unmodifiableMap(datasets); }
}
