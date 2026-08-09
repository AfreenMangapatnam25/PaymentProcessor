package com.paymentprocessor.ledgerservice.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProvisionMerchantAccountsRequest(
        @NotBlank String merchantId,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}
