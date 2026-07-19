package com.paymentprocessor.reconciliationservice.dto;

import com.paymentprocessor.reconciliationservice.domain.RecordSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A single record to attach to a run (internal or external). */
public record ReconRecordRequest(
        @NotNull RecordSource source,
        @Size(max = 64) String sourceSystem,
        @Size(max = 128) String externalReference,
        @Size(max = 64) String arn,
        @Size(max = 64) String internalPaymentId,
        @NotNull BigDecimal amount,
        BigDecimal feeAmount,
        @NotNull @Size(min = 3, max = 3) String currency,
        @NotNull LocalDate transactionDate,
        LocalDate valueDate,
        @Size(max = 64) String merchantId,
        @Size(max = 128) String counterparty,
        @Size(max = 8) String cardBin,
        @Size(max = 4) String cardLast4,
        @Size(max = 32) String transactionStatus,
        String rawPayload
) {
}
