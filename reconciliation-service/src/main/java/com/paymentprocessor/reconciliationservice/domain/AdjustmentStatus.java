package com.paymentprocessor.reconciliationservice.domain;

/** Lifecycle status of an adjustment. */
public enum AdjustmentStatus {
    PENDING,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    POSTED
}
