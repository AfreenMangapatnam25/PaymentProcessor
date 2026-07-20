package com.paymentprocessor.ledgerservice.web.dto;

import com.paymentprocessor.ledgerservice.domain.enums.EntryDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * A single debit or credit line in a posting request.
 * {@code currency} is optional; when omitted the account's own currency is used.
 */
public record JournalLineRequest(
        @NotBlank String accountId,
        @NotNull EntryDirection direction,
        @NotNull @Positive Long amountMinor,
        String currency,
        String description
) {
}
