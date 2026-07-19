package com.paymentprocessor.disputeservice.event;

import java.time.Instant;

/**
 * Published when a dispute is resolved against the merchant; the chargeback
 * stands and liability is confirmed.
 */
public record DisputeLostEvent(
        String disputeId,
        String merchantId,
        long amountLostMinor,
        String currency,
        String reason,
        Instant occurredAt) implements DisputeDomainEvent {
}
