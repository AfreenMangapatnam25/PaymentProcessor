package com.paymentprocessor.ledgerservice.domain.enums;

/**
 * Reason codes for reversing a posted journal (README §Reversals).
 */
public enum ReversalReason {
    ERRONEOUS_ENTRY,
    DUPLICATE_POSTING,
    TRANSACTION_REVERSED,
    ADJUSTMENT,
    AUDIT_CORRECTION
}
