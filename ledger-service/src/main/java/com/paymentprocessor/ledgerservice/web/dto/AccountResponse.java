package com.paymentprocessor.ledgerservice.web.dto;

import com.paymentprocessor.ledgerservice.domain.enums.AccountClassification;
import com.paymentprocessor.ledgerservice.domain.enums.AccountStatus;
import com.paymentprocessor.ledgerservice.domain.enums.NormalBalance;
import java.time.Instant;

public record AccountResponse(
        String id,
        String accountCode,
        String name,
        String ownerType,
        String ownerId,
        String typeCode,
        AccountClassification classification,
        NormalBalance normalBalance,
        String currency,
        String parentAccountId,
        AccountStatus status,
        Instant createdAt
) {
}
