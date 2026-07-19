package com.paymentprocessor.reconciliationservice.dto;

import com.paymentprocessor.reconciliationservice.domain.BankStatement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BankStatementResponse(
        Long id,
        String statementReference,
        String bankName,
        String accountType,
        String accountNumber,
        String format,
        String currency,
        LocalDate statementDate,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        int recordCount,
        String status,
        Instant ingestedAt
) {
    public static BankStatementResponse from(BankStatement s) {
        return new BankStatementResponse(
                s.getId(), s.getStatementReference(), s.getBankName(), s.getAccountType().name(),
                s.getAccountNumber(), s.getFormat().name(), s.getCurrency(), s.getStatementDate(),
                s.getOpeningBalance(), s.getClosingBalance(), s.getRecordCount(), s.getStatus().name(),
                s.getIngestedAt());
    }
}
