package com.paymentprocessor.authorization.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request to authorize a payment. Sent by the Payment Service after validation, fraud and limit
 * checks have already passed. Card data must be tokenized upstream; raw PANs are never accepted.
 */
public record AuthorizationRequest(

        @NotBlank String paymentReference,

        @NotBlank String merchantId,

        String customerId,

        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 4)
        BigDecimal amount,

        @NotBlank @Size(min = 3, max = 3) String currency,

        @NotBlank String paymentMethodToken,

        String cardBin,
        String cardLast4,
        Integer cardExpMonth,
        Integer cardExpYear,

        boolean captureImmediately,

        String statementDescriptor,

        Double riskScore,

        Map<String, String> metadata
) {
}
