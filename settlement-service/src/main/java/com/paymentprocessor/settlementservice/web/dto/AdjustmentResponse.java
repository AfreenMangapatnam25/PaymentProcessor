package com.paymentprocessor.settlementservice.web.dto;

import com.paymentprocessor.settlementservice.enums.AdjustmentStatus;
import com.paymentprocessor.settlementservice.enums.AdjustmentType;
import com.paymentprocessor.settlementservice.enums.ApprovalLevel;
import java.time.Instant;

/** API representation of a settlement adjustment. */
public record AdjustmentResponse(
        String id,
        String merchantId,
        AdjustmentType type,
        long amountMinor,
        String currency,
        String reasonCode,
        String description,
        AdjustmentStatus status,
        ApprovalLevel requiredApprovalLevel,
        String requestedBy,
        String approvedBy,
        Instant approvedAt,
        String appliedBatchId,
        Instant appliedAt,
        Instant createdAt
) {
}
