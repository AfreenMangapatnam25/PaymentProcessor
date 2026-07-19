package com.paymentprocessor.reconciliationservice.dto;

import com.paymentprocessor.reconciliationservice.domain.ExceptionRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExceptionResponse(
        Long id,
        UUID uuid,
        Long reconRunId,
        Long reconRecordId,
        String category,
        BigDecimal severityScore,
        String severityLevel,
        String status,
        String reviewQueue,
        BigDecimal amount,
        String currency,
        BigDecimal expectedAmount,
        BigDecimal actualAmount,
        String externalReference,
        String description,
        int ageDays,
        Instant detectedAt,
        Instant slaDueAt,
        String assignedTo,
        String resolutionType,
        String resolutionNote,
        String resolvedBy,
        Instant resolvedAt,
        boolean autoResolved
) {
    public static ExceptionResponse from(ExceptionRecord e) {
        return new ExceptionResponse(
                e.getId(), e.getUuid(), e.getReconRun().getId(),
                e.getReconRecord() == null ? null : e.getReconRecord().getId(),
                e.getCategory().name(), e.getSeverityScore(), e.getSeverityLevel().name(),
                e.getStatus().name(), e.getReviewQueue() == null ? null : e.getReviewQueue().name(),
                e.getAmount(), e.getCurrency(), e.getExpectedAmount(), e.getActualAmount(),
                e.getExternalReference(), e.getDescription(), e.getAgeDays(), e.getDetectedAt(),
                e.getSlaDueAt(), e.getAssignedTo(),
                e.getResolutionType() == null ? null : e.getResolutionType().name(),
                e.getResolutionNote(), e.getResolvedBy(), e.getResolvedAt(), e.isAutoResolved());
    }
}
