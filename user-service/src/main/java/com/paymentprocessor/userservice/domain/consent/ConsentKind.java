package com.paymentprocessor.userservice.domain.consent;

/**
 * The processing purposes a subject can grant or revoke consent for. Mirrors the
 * {@code consents.consent_kind} check constraint.
 */
public enum ConsentKind {
    MARKETING,
    DATA_PROCESSING,
    THIRD_PARTY_SHARING,
    PROFILING
}
