package com.paymentprocessor.settlementservice.web.dto;

import com.paymentprocessor.settlementservice.enums.FailureCategory;
import com.paymentprocessor.settlementservice.enums.PayoutStatus;
import com.paymentprocessor.settlementservice.enums.Rail;
import java.time.Instant;

/** API representation of a payout. */
public record PayoutResponse(
        String id,
        String batchId,
        String merchantId,
        String payoutAccountId,
        long amountMinor,
        String currency,
        Rail rail,
        PayoutStatus status,
        String providerRef,
        String ledgerJournalId,
        int attemptCount,
        Instant nextRetryAt,
        String failureCode,
        String failureReason,
        FailureCategory failureCategory,
        Instant scheduledAt,
        Instant submittedAt,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt
) {
}
