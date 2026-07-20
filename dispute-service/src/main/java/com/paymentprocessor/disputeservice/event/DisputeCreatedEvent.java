package com.paymentprocessor.disputeservice.event;

import com.paymentprocessor.disputeservice.domain.enums.Network;
import java.time.Instant;

/**
 * Published when a new dispute is recorded in the system.
 */
public record DisputeCreatedEvent(
        String disputeId,
        String merchantId,
        String chargebackId,
        Network network,
        String reasonCode,
        long amountMinor,
        String currency,
        Instant deadlineAt,
        Instant occurredAt) implements DisputeDomainEvent {
}
