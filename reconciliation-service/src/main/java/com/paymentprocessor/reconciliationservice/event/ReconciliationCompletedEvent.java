package com.paymentprocessor.reconciliationservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a reconciliation run finishes execution. Consumed by Finance, Operations,
 * Reporting and Audit services.
 */
public record ReconciliationCompletedEvent(
        UUID runUuid,
        String reconType,
        String channel,
        String businessDate,
        String status,
        long totalInternal,
        long totalExternal,
        long matchedCount,
        long mismatchedCount,
        long missingInternalCount,
        long missingExternalCount,
        long duplicateCount,
        long exceptionCount,
        BigDecimal matchRate,
        Instant completedAt
) {
}
