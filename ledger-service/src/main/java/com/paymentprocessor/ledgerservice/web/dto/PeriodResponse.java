package com.paymentprocessor.ledgerservice.web.dto;

import com.paymentprocessor.ledgerservice.domain.enums.PeriodState;
import com.paymentprocessor.ledgerservice.domain.enums.PeriodType;
import java.time.Instant;
import java.time.LocalDate;

public record PeriodResponse(
        String id,
        String code,
        PeriodType periodType,
        LocalDate startDate,
        LocalDate endDate,
        PeriodState state,
        Instant createdAt,
        Instant closedAt
) {
}
