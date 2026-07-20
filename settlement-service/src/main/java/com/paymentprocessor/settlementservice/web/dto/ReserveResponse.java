package com.paymentprocessor.settlementservice.web.dto;

import com.paymentprocessor.settlementservice.enums.ReserveKind;
import com.paymentprocessor.settlementservice.enums.ReserveStatus;
import java.time.Instant;
import java.time.LocalDate;

/** API representation of a reserve hold. */
public record ReserveResponse(
        String id,
        String merchantId,
        ReserveKind kind,
        Integer rateBps,
        long amountMinor,
        long releasedMinor,
        long remainingMinor,
        String currency,
        String sourceBatchId,
        LocalDate holdUntil,
        ReserveStatus status,
        Instant releasedAt,
        Instant createdAt
) {
}
