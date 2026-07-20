package com.paymentprocessor.ledgerservice.web.dto;

import com.paymentprocessor.ledgerservice.domain.enums.ReversalReason;
import jakarta.validation.constraints.NotNull;

/**
 * Request to reverse a posted journal by generating a compensating journal.
 * {@code approvedBy} is required when any single line exceeds the configured
 * approval threshold.
 */
public record ReverseJournalRequest(
        @NotNull ReversalReason reason,
        String description,
        String createdBy,
        String approvedBy,
        String idempotencyKey
) {
}
