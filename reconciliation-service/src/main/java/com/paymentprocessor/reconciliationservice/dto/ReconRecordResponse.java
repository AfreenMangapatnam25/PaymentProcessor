package com.paymentprocessor.reconciliationservice.dto;

import com.paymentprocessor.reconciliationservice.domain.ReconRecord;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReconRecordResponse(
        Long id,
        String source,
        String sourceSystem,
        String externalReference,
        String arn,
        String internalPaymentId,
        BigDecimal amount,
        BigDecimal feeAmount,
        String currency,
        LocalDate transactionDate,
        LocalDate valueDate,
        String merchantId,
        String counterparty,
        String cardBin,
        String cardLast4,
        String transactionStatus,
        String matchStatus
) {
    public static ReconRecordResponse from(ReconRecord r) {
        return new ReconRecordResponse(
                r.getId(), r.getSource().name(), r.getSourceSystem(), r.getExternalReference(), r.getArn(),
                r.getInternalPaymentId(), r.getAmount(), r.getFeeAmount(), r.getCurrency(),
                r.getTransactionDate(), r.getValueDate(), r.getMerchantId(), r.getCounterparty(),
                r.getCardBin(), r.getCardLast4(), r.getTransactionStatus(), r.getMatchStatus().name());
    }
}
