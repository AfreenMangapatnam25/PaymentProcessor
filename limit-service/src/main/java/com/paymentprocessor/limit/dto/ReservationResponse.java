package com.paymentprocessor.limit.dto;

import com.paymentprocessor.limit.domain.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Result of a reserve/commit/release operation.
 */
public record ReservationResponse(
        String reservationId,
        String transactionId,
        ReservationStatus status,
        String currency,
        BigDecimal reservedAmount,
        BigDecimal committedAmount,
        Instant expiresAt,
        List<LimitViolationDto> softViolations
) {}
