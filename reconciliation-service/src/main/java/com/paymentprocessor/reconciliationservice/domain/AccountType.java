package com.paymentprocessor.reconciliationservice.domain;

/** Bank account category being reconciled. */
public enum AccountType {
    SETTLEMENT,
    OPERATING,
    RESERVE,
    ESCROW,
    SWEEP
}
