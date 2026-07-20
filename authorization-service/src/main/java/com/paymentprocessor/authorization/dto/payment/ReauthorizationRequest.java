package com.paymentprocessor.authorization.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Request to re-authorize a payment whose original authorization has expired or was insufficient.
 * A new authorization is created and linked to the original. A null {@code amount} re-uses the
 * original requested amount. A tokenized payment method is required (raw PANs are never accepted).
 */
public record ReauthorizationRequest(
        @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotBlank String paymentMethodToken,
        String reason
) {
}
