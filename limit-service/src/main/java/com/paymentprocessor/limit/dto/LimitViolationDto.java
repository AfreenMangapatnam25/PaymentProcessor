package com.paymentprocessor.limit.dto;

import java.math.BigDecimal;

/**
 * A single limit that was breached during evaluation.
 */
public record LimitViolationDto(
        String limitConfigId,
        String limitName,
        String dimension,
        String timeWindow,
        String enforcement,
        BigDecimal threshold,
        BigDecimal currentUsage,
        BigDecimal attempted,
        String message
) {}
