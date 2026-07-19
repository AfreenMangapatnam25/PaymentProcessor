package com.paymentprocessor.ledgerservice.web.dto;

import com.paymentprocessor.ledgerservice.domain.enums.HoldStatus;
import java.time.Instant;

public record HoldResponse(
        String id,
        String accountId,
        long amountMinor,
        String currency,
        String reason,
        HoldStatus status,
        String externalRef,
        Instant expiresAt,
        Instant createdAt,
        Instant releasedAt
) {
}
