package com.paymentprocessor.ledgerservice.event;

import java.time.Instant;
import java.util.List;

/**
 * Published when a journal is successfully posted to the ledger.
 */
public record LedgerPostedEvent(
        String journalId,
        String eventType,
        String externalRef,
        Instant postedAt,
        List<String> accountIds,
        long totalDebitMinor,
        long totalCreditMinor
) {
    public static final String TYPE = "LedgerPosted";
}
