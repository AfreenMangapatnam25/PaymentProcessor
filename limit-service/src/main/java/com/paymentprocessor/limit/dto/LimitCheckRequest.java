package com.paymentprocessor.limit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request to evaluate a prospective transaction against the applicable limits
 * without holding any capacity (a dry run).
 */
public record LimitCheckRequest(

        @Size(max = 100)
        String customerId,

        @Size(max = 100)
        String merchantId,

        @NotNull
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO-4217 code")
        String currency,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = true, message = "amount must be non-negative")
        BigDecimal amount,

        /** ISO country code involved in the transaction (optional). */
        @Size(max = 2)
        String country,

        @Size(max = 100)
        String transactionId
) {}
