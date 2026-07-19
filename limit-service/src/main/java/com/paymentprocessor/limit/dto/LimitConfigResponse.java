package com.paymentprocessor.limit.dto;

import com.paymentprocessor.limit.domain.entity.LimitConfiguration;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * View of a limit configuration.
 */
public record LimitConfigResponse(
        String id,
        String name,
        String scope,
        String scopeId,
        String dimension,
        String timeWindow,
        BigDecimal threshold,
        String currency,
        String enforcement,
        int priority,
        boolean active,
        String timeZone,
        Instant createdAt,
        Instant updatedAt
) {
    public static LimitConfigResponse from(LimitConfiguration c) {
        return new LimitConfigResponse(
                c.getId().toString(),
                c.getName(),
                c.getScope().name(),
                c.getScopeId(),
                c.getDimension().name(),
                c.getTimeWindow().name(),
                c.getThreshold(),
                c.getCurrency(),
                c.getEnforcement().name(),
                c.getPriority(),
                c.isActive(),
                c.getTimeZone(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
