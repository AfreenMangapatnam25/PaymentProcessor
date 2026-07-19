package com.paymentprocessor.settlementservice.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle states of an individual merchant payout.
 */
public enum PayoutStatus {
    /** Created, awaiting submission to a rail. */
    PENDING,
    /** Transfer instruction submitted to the bank / rail. */
    INITIATED,
    /** Rail acknowledged and is processing. */
    PROCESSING,
    /** Funds confirmed deposited. */
    COMPLETED,
    /** Scheduled for a future retry after a transient/recoverable failure. */
    RETRY_SCHEDULED,
    /** Failed permanently after exhausting retries or a fatal error. */
    FAILED,
    /** Funds returned by the receiving bank (invalid/closed account, etc.). */
    RETURNED,
    /** Payout reversed to recover funds. */
    REVERSED;

    private static final Set<PayoutStatus> TERMINAL =
            EnumSet.of(COMPLETED, FAILED, RETURNED, REVERSED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean canTransitionTo(PayoutStatus target) {
        return switch (this) {
            case PENDING -> target == INITIATED || target == FAILED;
            case INITIATED -> target == PROCESSING || target == COMPLETED
                    || target == RETRY_SCHEDULED || target == FAILED || target == RETURNED;
            case PROCESSING -> target == COMPLETED || target == RETRY_SCHEDULED
                    || target == FAILED || target == RETURNED;
            case RETRY_SCHEDULED -> target == INITIATED || target == FAILED;
            case COMPLETED -> target == RETURNED || target == REVERSED;
            case RETURNED -> target == REVERSED || target == INITIATED; // re-attempt after fix
            case FAILED -> target == REVERSED;
            case REVERSED -> false;
        };
    }
}
