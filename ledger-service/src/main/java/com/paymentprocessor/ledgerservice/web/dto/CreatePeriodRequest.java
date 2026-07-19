package com.paymentprocessor.ledgerservice.web.dto;

import com.paymentprocessor.ledgerservice.domain.enums.PeriodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreatePeriodRequest(
        @NotBlank String code,
        @NotNull PeriodType periodType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
