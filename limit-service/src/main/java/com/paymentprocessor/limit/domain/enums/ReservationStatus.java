package com.paymentprocessor.limit.domain.enums;

/**
 * Lifecycle of a limit reservation.
 * RESERVED  — capacity held, awaiting capture or release.
 * COMMITTED — capacity converted to committed usage on capture.
 * RELEASED  — capacity freed (failure, cancellation, manual override).
 * EXPIRED   — reservation timed out and was auto-released.
 */
public enum ReservationStatus {
    RESERVED,
    COMMITTED,
    RELEASED,
    EXPIRED
}
