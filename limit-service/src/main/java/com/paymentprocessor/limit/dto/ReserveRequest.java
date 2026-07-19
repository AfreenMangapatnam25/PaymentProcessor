package com.paymentprocessor.limit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request to evaluate and, if within bounds, reserve capacity against the
 * applicable limits for a transaction.
 */
public record ReserveRequest(

        @NotBlank
        @Size(max = 100)
        String transactionId,

        @Size(max = 100)
        String customerId,

        @Size(max = 100)
        String merchantId,

        @NotNull
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO-4217 code")
        String currency,

        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be positive")
        BigDecimal amount,

        @Size(max = 2)
        String country,

        /** Optional idempotency key so retried reserve calls are safe. */
        @Size(max = 100)
        String idempotencyKey
) {}
