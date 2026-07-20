package com.paymentprocessor.ledgerservice.web.dto;

import com.paymentprocessor.ledgerservice.domain.enums.NormalBalance;
import java.time.Instant;

public record BalanceResponse(
        String accountId,
        String currency,
        NormalBalance normalBalance,
        long postedMinor,
        long pendingMinor,
        long heldMinor,
        long availableMinor,
        long entryHighWater,
        int version,
        Instant updatedAt
) {
}
