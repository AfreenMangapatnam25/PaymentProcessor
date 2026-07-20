package com.paymentprocessor.reconciliationservice.dto;

import com.paymentprocessor.reconciliationservice.domain.Adjustment;

import java.math.BigDecimal;
import java.time.Instant;

public record AdjustmentResponse(
        Long id,
        Long exceptionId,
        String adjustmentType,
        BigDecimal amount,
        String currency,
        String reason,
        String ledgerReference,
        String status,
        boolean requiresDualApproval,
        String createdBy,
        String approvedBy,
        Instant approvedAt,
        Instant postedAt,
        Instant createdAt
) {
    public static AdjustmentResponse from(Adjustment a) {
        return new AdjustmentResponse(
                a.getId(), a.getExceptionRecord().getId(), a.getAdjustmentType().name(), a.getAmount(),
                a.getCurrency(), a.getReason(), a.getLedgerReference(), a.getStatus().name(),
                a.isRequiresDualApproval(), a.getCreatedBy(), a.getApprovedBy(), a.getApprovedAt(),
                a.getPostedAt(), a.getCreatedAt());
    }
}
