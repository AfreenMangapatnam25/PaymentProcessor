package com.paymentprocessor.ledgerservice.event;

import java.time.Instant;

/**
 * Published when a posted journal is reversed by a compensating journal.
 */
public record LedgerReversedEvent(
        String reversalJournalId,
        String originalJournalId,
        String reason,
        Instant postedAt
) {
    public static final String TYPE = "LedgerReversed";
}
