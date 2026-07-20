package com.paymentprocessor.settlementservice.web.dto;

import com.paymentprocessor.settlementservice.enums.BatchStatus;
import com.paymentprocessor.settlementservice.enums.ScheduleType;
import java.time.Instant;

/** API representation of a settlement batch. */
public record BatchResponse(
        String id,
        String merchantId,
        String currency,
        ScheduleType scheduleType,
        BatchStatus status,
        Instant periodStart,
        Instant periodEnd,
        long grossMinor,
        long refundsMinor,
        long chargebacksMinor,
        long feesMinor,
        long interchangeMinor,
        long reserveMinor,
        long settlementFeeMinor,
        long adjustmentsMinor,
        long netMinor,
        String ledgerJournalId,
        String approvedBy,
        Instant approvedAt,
        Instant closedAt,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}
