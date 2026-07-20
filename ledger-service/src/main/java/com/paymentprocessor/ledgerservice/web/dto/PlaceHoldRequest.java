package com.paymentprocessor.ledgerservice.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record PlaceHoldRequest(
        @NotBlank String accountId,
        @NotNull @Positive Long amountMinor,
        String currency,
        String reason,
        String externalRef,
        Instant expiresAt
) {
}
