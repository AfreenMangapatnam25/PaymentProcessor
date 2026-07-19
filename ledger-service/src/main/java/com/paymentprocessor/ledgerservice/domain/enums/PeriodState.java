package com.paymentprocessor.ledgerservice.domain.enums;

/**
 * Lifecycle of an accounting period: OPEN -> CLOSING -> CLOSED -> LOCKED.
 */
public enum PeriodState {
    OPEN,
    CLOSING,
    CLOSED,
    LOCKED;

    /** Whether ordinary journal entries may be posted into a period in this state. */
    public boolean acceptsPostings() {
        return this == OPEN || this == CLOSING;
    }
}
