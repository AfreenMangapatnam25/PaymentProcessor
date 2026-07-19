package com.paymentprocessor.ledgerservice.web.dto;

import com.paymentprocessor.ledgerservice.domain.enums.JournalStatus;
import com.paymentprocessor.ledgerservice.domain.enums.ReversalReason;
import java.time.Instant;
import java.util.List;

public record JournalResponse(
        String id,
        String eventType,
        String externalRef,
        String idempotencyKey,
        String description,
        JournalStatus status,
        String reversesJournalId,
        String reversedByJournalId,
        ReversalReason reversalReason,
        String periodId,
        Instant effectiveAt,
        Instant postedAt,
        String createdBy,
        List<EntryResponse> lines,
        long totalDebitMinor,
        long totalCreditMinor,
        boolean balanced
) {
}
