package com.paymentprocessor.ledgerservice.web.dto;

import com.paymentprocessor.ledgerservice.domain.enums.EntryDirection;
import java.time.Instant;

public record StatementLine(
        Long entryId,
        String journalId,
        Integer lineNumber,
        EntryDirection direction,
        long amountMinor,
        String currency,
        String description,
        Instant effectiveAt,
        long runningBalanceMinor
) {
}
