package com.paymentprocessor.fraudservice.engine;

/**
 * Final verdict types enforced by downstream services.
 */
public enum Decision {
    /** Cleared; proceed to authorization. */
    APPROVE,
    /** Require step-up authentication (3DS, OTP, biometric) before proceeding. */
    CHALLENGE,
    /** Hold transaction pending manual analyst review. */
    REVIEW,
    /** Blocked; return decline reason to merchant. */
    DECLINE,
    /** Immediate compliance/security alert (sanctions hit, confirmed fraud ring). */
    ESCALATE
}
