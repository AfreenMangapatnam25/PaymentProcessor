package com.paymentprocessor.limit.domain.enums;

/**
 * Outcome of evaluating a transaction against the applicable limits.
 * APPROVED  — within all hard limits.
 * DECLINED  — one or more hard limits exceeded; transaction must be blocked.
 * FLAGGED   — a soft limit was exceeded; transaction may proceed with an alert.
 */
public enum LimitDecision {
    APPROVED,
    DECLINED,
    FLAGGED
}
