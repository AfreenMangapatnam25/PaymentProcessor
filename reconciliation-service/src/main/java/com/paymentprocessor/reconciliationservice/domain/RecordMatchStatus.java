package com.paymentprocessor.reconciliationservice.domain;

/** Match state of an individual reconciliation record within a run. */
public enum RecordMatchStatus {
    UNMATCHED,
    MATCHED,
    EXCEPTION
}
