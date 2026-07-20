package com.paymentprocessor.settlementservice.web.dto;

import com.paymentprocessor.settlementservice.enums.AdjustmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Request to create a manual settlement adjustment. */
public record CreateAdjustmentRequest(
        @NotBlank String merchantId,
        @NotNull AdjustmentType type,
        @Positive long amountMinor,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotBlank String reasonCode,
        String description,
        @NotBlank String requestedBy
) {
}
