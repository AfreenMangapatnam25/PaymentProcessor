package com.paymentprocessor.settlementservice.enums;

/**
 * Classification of a payout failure, which drives retry behaviour.
 */
public enum FailureCategory {
    /** Bank timeout, network error, rate limit -> retry with exponential backoff. */
    TRANSIENT(true),
    /** Insufficient platform funds, daily limit exceeded -> retry next business day. */
    RECOVERABLE(true),
    /** Invalid account, account closed, name mismatch -> no retry, alert merchant. */
    MERCHANT_SIDE(false),
    /** Bank holiday, rail maintenance -> retry next business day. */
    RAIL_SIDE(true),
    /** Sanctions hit, regulatory block -> no retry, escalate to compliance. */
    FATAL(false);

    private final boolean retryable;

    FailureCategory(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
