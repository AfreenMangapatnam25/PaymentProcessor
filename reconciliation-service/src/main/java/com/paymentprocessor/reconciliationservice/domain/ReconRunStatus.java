package com.paymentprocessor.reconciliationservice.domain;

/** Lifecycle status of a reconciliation run. */
public enum ReconRunStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
