package com.paymentprocessor.reconciliationservice.domain;

/** Processing status of an ingested statement. */
public enum StatementStatus {
    INGESTED,
    NORMALIZED,
    RECONCILED,
    FAILED
}
