package com.paymentprocessor.settlementservice.web.dto;

import com.paymentprocessor.settlementservice.enums.SettlementItemType;
import java.time.Instant;

/** API representation of a settlement line item. */
public record ItemResponse(
        Long id,
        String batchId,
        String merchantId,
        SettlementItemType type,
        String sourceType,
        String sourceId,
        long amountMinor,
        long signedAmountMinor,
        String currency,
        Instant effectiveAt
) {
}
