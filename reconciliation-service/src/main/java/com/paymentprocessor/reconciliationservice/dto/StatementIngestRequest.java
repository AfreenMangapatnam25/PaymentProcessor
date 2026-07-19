package com.paymentprocessor.reconciliationservice.dto;

import com.paymentprocessor.reconciliationservice.domain.AccountType;
import com.paymentprocessor.reconciliationservice.domain.StatementFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Request to ingest a CSV statement and attach its rows to a run as EXTERNAL records. */
public record StatementIngestRequest(
        @NotBlank @Size(max = 128) String statementReference,
        @NotBlank @Size(max = 128) String bankName,
        @NotNull AccountType accountType,
        @NotBlank @Size(max = 64) String accountNumber,
        @NotNull StatementFormat format,
        @NotNull @Size(min = 3, max = 3) String currency,
        @NotNull LocalDate statementDate,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        @NotBlank String csvContent
) {
}
