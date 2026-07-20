package com.paymentprocessor.reconciliationservice.domain;

/** Human-facing severity bucket derived from the numeric severity score. */
public enum SeverityLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /** Map a 0-100 severity score to a level per the README scoring bands. */
    public static SeverityLevel fromScore(double score) {
        if (score >= 91) return CRITICAL;
        if (score >= 61) return HIGH;
        if (score >= 31) return MEDIUM;
        return LOW;
    }
}
