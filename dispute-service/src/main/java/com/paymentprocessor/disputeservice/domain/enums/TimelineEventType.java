package com.paymentprocessor.disputeservice.domain.enums;

/**
 * Types of entries recorded on a dispute's audit / status timeline.
 */
public enum TimelineEventType {
    DISPUTE_CREATED,
    MERCHANT_NOTIFIED,
    EVIDENCE_REQUESTED,
    EVIDENCE_UPLOADED,
    EVIDENCE_REVIEWED,
    REPRESENTMENT_SUBMITTED,
    ISSUER_RESPONSE_RECEIVED,
    PRE_ARBITRATION_RECEIVED,
    ARBITRATION_FILED,
    ARBITRATION_DECISION,
    DISPUTE_WON,
    DISPUTE_LOST,
    DISPUTE_ACCEPTED,
    DEADLINE_APPROACHING,
    DEADLINE_MISSED,
    STATUS_CHANGED,
    LEDGER_POSTED
}
