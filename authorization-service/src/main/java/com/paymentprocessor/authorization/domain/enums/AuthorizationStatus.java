package com.paymentprocessor.authorization.domain.enums;

/**
 * Lifecycle state of a payment authorization.
 */
public enum AuthorizationStatus {
    /** Created but not yet sent to / answered by the gateway. */
    PENDING,
    /** Fully approved by the issuer; funds are held. */
    APPROVED,
    /** Approved for less than the requested amount. */
    PARTIALLY_APPROVED,
    /** Declined by the issuer or gateway. */
    DECLINED,
    /** Step-up authentication (e.g. 3-D Secure) is required to proceed. */
    REQUIRES_AUTHENTICATION,
    /** The held funds have been fully captured. */
    CAPTURED,
    /** Part of the held amount has been captured; remainder still held or released. */
    PARTIALLY_CAPTURED,
    /** The authorization hold was released (voided) before capture. */
    REVERSED,
    /** The hold lapsed without capture. */
    EXPIRED,
    /** A terminal error prevented a verdict. */
    FAILED;

    public boolean isApproved() {
        return this == APPROVED || this == PARTIALLY_APPROVED;
    }

    public boolean isCapturable() {
        return this == APPROVED || this == PARTIALLY_APPROVED || this == PARTIALLY_CAPTURED;
    }

    public boolean isReversible() {
        return this == APPROVED || this == PARTIALLY_APPROVED || this == PARTIALLY_CAPTURED;
    }

    public boolean isTerminal() {
        return this == DECLINED || this == CAPTURED || this == REVERSED
                || this == EXPIRED || this == FAILED;
    }
}
