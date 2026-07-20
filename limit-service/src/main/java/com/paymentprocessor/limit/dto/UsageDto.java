package com.paymentprocessor.limit.dto;

import java.math.BigDecimal;

/**
 * Current consumption of one limit for an entity, including how much headroom
 * remains in the active window.
 */
public record UsageDto(
        String limitConfigId,
        String limitName,
        String dimension,
        String timeWindow,
        String windowKey,
        BigDecimal threshold,
        BigDecimal used,
        BigDecimal remaining,
        String currency,
        String enforcement
) {}
