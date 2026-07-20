package com.paymentprocessor.reconciliationservice.dto;

import com.paymentprocessor.reconciliationservice.domain.AdjustmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Request to create a corrective adjustment for an exception. */
public record AdjustmentCreateRequest(
        @NotNull AdjustmentType adjustmentType,
        @NotNull BigDecimal amount,
        @NotNull @Size(min = 3, max = 3) String currency,
        @NotBlank @Size(max = 1024) String reason,
        @NotBlank @Size(max = 128) String createdBy
) {
}
