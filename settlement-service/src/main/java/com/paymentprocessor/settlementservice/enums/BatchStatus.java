package com.paymentprocessor.settlementservice.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle states of a settlement batch. The {@link #canTransitionTo(BatchStatus)}
 * method enforces the legal state machine described in the service README.
 */
public enum BatchStatus {
    /** Calculated and scheduled, awaiting execution window. */
    PENDING,
    /** Passed validation and (auto- or manually) approved, ready to initiate. */
    APPROVED,
    /** Transfer instruction sent to the bank / payment rail. */
    INITIATED,
    /** Bank or rail acknowledged and is processing the transfer. */
    PROCESSING,
    /** Funds successfully deposited to the merchant account. */
    COMPLETED,
    /** Failed after all retry attempts; requires manual intervention. */
    FAILED,
    /** Internal records match the external bank statement. */
    RECONCILED,
    /** Settlement was reversed due to error, fraud, or adjustment. */
    REVERSED,
    /** Below minimum payout threshold; rolled into a later cycle. */
    ROLLED_OVER;

    private static final Set<BatchStatus> TERMINAL =
            EnumSet.of(COMPLETED, FAILED, REVERSED, RECONCILED, ROLLED_OVER);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean canTransitionTo(BatchStatus target) {
        return switch (this) {
            case PENDING -> target == APPROVED || target == ROLLED_OVER || target == FAILED;
            case APPROVED -> target == INITIATED || target == FAILED;
            case INITIATED -> target == PROCESSING || target == COMPLETED || target == FAILED;
            case PROCESSING -> target == COMPLETED || target == FAILED;
            case COMPLETED -> target == RECONCILED || target == REVERSED;
            case FAILED -> target == INITIATED || target == REVERSED; // retry re-initiates
            case RECONCILED -> target == REVERSED;
            case REVERSED, ROLLED_OVER -> false;
        };
    }
}
