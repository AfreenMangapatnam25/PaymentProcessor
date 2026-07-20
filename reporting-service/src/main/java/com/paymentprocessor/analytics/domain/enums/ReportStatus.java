package com.paymentprocessor.analytics.domain.enums;

/** Lifecycle of an async report job. */
public enum ReportStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    EXPIRED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == EXPIRED;
    }
}
