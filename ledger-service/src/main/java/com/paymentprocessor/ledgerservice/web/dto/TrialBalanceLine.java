package com.paymentprocessor.ledgerservice.web.dto;

import com.paymentprocessor.ledgerservice.domain.enums.AccountClassification;

public record TrialBalanceLine(
        String accountCode,
        String accountName,
        AccountClassification classification,
        long debitMinor,
        long creditMinor
) {
}
