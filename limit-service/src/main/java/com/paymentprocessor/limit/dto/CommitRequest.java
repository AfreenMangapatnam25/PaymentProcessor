package com.paymentprocessor.limit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request to commit (capture) a reservation. {@code capturedAmount} may be less
 * than the reserved amount for a partial capture — the uncaptured remainder is
 * released back to the available pool.
 */
public record CommitRequest(

        @NotNull
        @DecimalMin(value = "0.01", message = "capturedAmount must be positive")
        BigDecimal capturedAmount
) {}
