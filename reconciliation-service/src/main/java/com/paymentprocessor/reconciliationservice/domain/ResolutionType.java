package com.paymentprocessor.reconciliationservice.domain;

/** How an exception was resolved. */
public enum ResolutionType {
    MATCHED,
    ENTRY_POSTED,
    REVERSED,
    ADJUSTED,
    ESCALATED,
    DEFERRED,
    AUTO_CORRECTED,
    WRITTEN_OFF
}
