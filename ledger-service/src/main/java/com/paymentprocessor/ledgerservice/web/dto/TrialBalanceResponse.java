package com.paymentprocessor.ledgerservice.web.dto;

import java.time.Instant;
import java.util.List;

public record TrialBalanceResponse(
        Instant asOf,
        List<TrialBalanceLine> lines,
        long totalDebitMinor,
        long totalCreditMinor,
        boolean balanced
) {
}
