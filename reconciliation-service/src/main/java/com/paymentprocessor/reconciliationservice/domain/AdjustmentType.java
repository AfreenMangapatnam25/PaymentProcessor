package com.paymentprocessor.reconciliationservice.domain;

/** Type of corrective adjustment posted to resolve an exception. */
public enum AdjustmentType {
    POST_ENTRY,
    REVERSE,
    FX_VARIANCE,
    FEE_ADJUSTMENT,
    WRITE_OFF
}
