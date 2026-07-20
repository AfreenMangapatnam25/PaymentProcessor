package com.paymentprocessor.ledgerservice.web.dto;

import com.paymentprocessor.ledgerservice.domain.enums.PeriodState;
import jakarta.validation.constraints.NotNull;

public record UpdatePeriodStateRequest(
        @NotNull PeriodState state
) {
}
