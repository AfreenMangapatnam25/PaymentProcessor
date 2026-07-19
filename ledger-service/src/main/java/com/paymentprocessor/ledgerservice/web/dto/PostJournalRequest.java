package com.paymentprocessor.ledgerservice.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Request to post a balanced double-entry journal.
 * A journal must contain at least two lines whose debits equal credits per currency.
 */
public record PostJournalRequest(
        String eventType,
        String externalRef,
        String idempotencyKey,
        String description,
        Instant effectiveAt,
        String createdBy,
        Map<String, Object> metadata,
        @NotEmpty(message = "a journal must have at least two lines")
        @Size(min = 2, message = "a journal must have at least two lines")
        @Valid List<JournalLineRequest> lines
) {
}
