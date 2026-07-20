package com.paymentprocessor.ledgerservice.web.dto;

import com.paymentprocessor.ledgerservice.domain.enums.EntryDirection;
import java.time.Instant;

public record EntryResponse(
        Long id,
        Integer lineNumber,
        String accountId,
        EntryDirection direction,
        long amountMinor,
        String currency,
        String description,
        Instant effectiveAt
) {
}
