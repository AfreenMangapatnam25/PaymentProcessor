package com.paymentprocessor.settlementservice.enums;

/** Approval authority required for an adjustment or reversal, by monetary threshold. */
public enum ApprovalLevel {
    /** No approval required (auto-approved). */
    AUTO,
    /** Requires a supervisor sign-off. */
    SUPERVISOR,
    /** Requires finance-director sign-off. */
    FINANCE_DIRECTOR
}
