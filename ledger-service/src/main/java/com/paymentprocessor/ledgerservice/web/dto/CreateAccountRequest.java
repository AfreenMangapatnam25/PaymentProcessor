package com.paymentprocessor.ledgerservice.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        String id,
        @NotBlank String accountCode,
        @NotBlank String name,
        @NotBlank String typeCode,
        @NotBlank @Size(min = 3, max = 3) String currency,
        String ownerType,
        String ownerId,
        String parentAccountId
) {
}
