package com.paymentprocessor.reconciliationservice.dto;

import com.paymentprocessor.reconciliationservice.domain.ReconRun;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReconRunResponse(
        Long id,
        UUID uuid,
        String reconType,
        String channel,
        String accountRef,
        LocalDate businessDate,
        String currency,
        String status,
        Instant startedAt,
        Instant completedAt,
        long totalInternal,
        long totalExternal,
        long matchedCount,
        long mismatchedCount,
        long missingInternalCount,
        long missingExternalCount,
        long duplicateCount,
        long exceptionCount,
        BigDecimal matchedAmount,
        BigDecimal matchRate,
        String triggeredBy,
        String failureReason,
        Instant createdAt
) {
    public static ReconRunResponse from(ReconRun r) {
        return new ReconRunResponse(
                r.getId(), r.getUuid(), r.getReconType().name(), r.getChannel(), r.getAccountRef(),
                r.getBusinessDate(), r.getCurrency(), r.getStatus().name(), r.getStartedAt(), r.getCompletedAt(),
                r.getTotalInternal(), r.getTotalExternal(), r.getMatchedCount(), r.getMismatchedCount(),
                r.getMissingInternalCount(), r.getMissingExternalCount(), r.getDuplicateCount(),
                r.getExceptionCount(), r.getMatchedAmount(), r.getMatchRate(), r.getTriggeredBy(),
                r.getFailureReason(), r.getCreatedAt());
    }
}
