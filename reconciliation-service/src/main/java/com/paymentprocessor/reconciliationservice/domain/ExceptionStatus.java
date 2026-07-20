package com.paymentprocessor.reconciliationservice.domain;

/** Lifecycle status of a reconciliation exception (mismatch case). */
public enum ExceptionStatus {
    OPEN,
    IN_REVIEW,
    RESOLVED,
    ESCALATED,
    DEFERRED
}
