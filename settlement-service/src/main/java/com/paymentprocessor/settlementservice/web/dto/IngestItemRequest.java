package com.paymentprocessor.settlementservice.web.dto;

import com.paymentprocessor.settlementservice.enums.SettlementItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request to ingest a settlement line item (capture, refund, chargeback, fee,
 * or adjustment) from an upstream service.
 */
public record IngestItemRequest(
        @NotBlank String merchantId,
        @NotNull SettlementItemType type,
        String sourceType,
        String sourceId,
        @Positive long amountMinor,
        @NotBlank @Size(min = 3, max = 3) String currency,
        Instant effectiveAt,
        String idempotencyKey
) {
}
