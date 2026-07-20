package com.paymentprocessor.authorization.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

/**
 * Request to capture an authorized hold. A null {@code amount} captures the full approved amount.
 */
public record CaptureRequest(
        @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 4) BigDecimal amount
) {
}
