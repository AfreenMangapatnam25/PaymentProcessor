package com.paymentprocessor.settlementservice.enums;

/** Lifecycle of a rolling / risk reserve hold. */
public enum ReserveStatus {
    /** Funds are held and not yet eligible for release. */
    HELD,
    /** Hold-until date reached; released back into a future settlement. */
    RELEASED,
    /** Reserve was consumed to cover a chargeback, reversal, or debit. */
    CONSUMED,
    /** Manually cancelled by the risk team. */
    CANCELLED
}
