package com.paymentprocessor.settlementservice.enums;

/** Approval lifecycle of a settlement adjustment. */
public enum AdjustmentStatus {
    /** Requested, awaiting the required approval level. */
    PENDING_APPROVAL,
    /** Approved and eligible to be applied to a settlement. */
    APPROVED,
    /** Applied to a settlement batch / payout. */
    APPLIED,
    /** Rejected by an approver. */
    REJECTED
}
