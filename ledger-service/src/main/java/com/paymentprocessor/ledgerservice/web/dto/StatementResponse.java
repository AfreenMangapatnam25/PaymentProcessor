package com.paymentprocessor.ledgerservice.web.dto;

import java.time.Instant;
import java.util.List;

public record StatementResponse(
        String accountId,
        String currency,
        Instant from,
        Instant to,
        long openingBalanceMinor,
        long closingBalanceMinor,
        List<StatementLine> lines
) {
}
