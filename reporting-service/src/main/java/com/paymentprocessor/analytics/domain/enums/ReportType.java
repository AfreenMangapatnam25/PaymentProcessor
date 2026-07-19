package com.paymentprocessor.analytics.domain.enums;

/** Merchant-facing report templates plus the generic ad-hoc query export. */
public enum ReportType {
    SETTLEMENT_SUMMARY,
    CHARGEBACK_REPORT,
    TRANSACTION_DETAIL,
    FEE_BREAKDOWN,
    AD_HOC
}
