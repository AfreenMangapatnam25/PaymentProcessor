package com.paymentprocessor.disputeservice.event;

import com.paymentprocessor.disputeservice.domain.enums.Network;
import java.time.Instant;

/**
 * Published when a chargeback notification has been processed and its financial
 * impact recorded.
 */
public record ChargebackReceivedEvent(
        String disputeId,
        String merchantId,
        Network network,
        long amountMinor,
        long feeMinor,
        String currency,
        String ledgerJournalId,
        Instant occurredAt) implements DisputeDomainEvent {
}
