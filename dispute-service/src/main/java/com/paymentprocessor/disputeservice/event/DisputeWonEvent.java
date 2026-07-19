package com.paymentprocessor.disputeservice.event;

import java.time.Instant;

/**
 * Published when a dispute is resolved in the merchant's favour; the chargeback
 * is reversed and funds are returned.
 */
public record DisputeWonEvent(
        String disputeId,
        String merchantId,
        long amountRecoveredMinor,
        String currency,
        String reversalJournalId,
        Instant occurredAt) implements DisputeDomainEvent {
}
