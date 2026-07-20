package com.paymentprocessor.limit.domain.enums;

/**
 * HARD — breaching the limit blocks the transaction.
 * SOFT — the transaction proceeds but an alert / event is generated.
 */
public enum EnforcementMode {
    HARD,
    SOFT
}
