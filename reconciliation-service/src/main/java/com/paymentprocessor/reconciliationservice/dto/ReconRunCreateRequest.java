package com.paymentprocessor.reconciliationservice.dto;

import com.paymentprocessor.reconciliationservice.domain.ReconType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Request payload to create a reconciliation run. */
public record ReconRunCreateRequest(
        @NotNull ReconType reconType,
        @NotBlank @Size(max = 128) String channel,
        @Size(max = 128) String accountRef,
        @NotNull LocalDate businessDate,
        @Size(max = 3) String currency,
        @Size(max = 128) String triggeredBy
) {
}
