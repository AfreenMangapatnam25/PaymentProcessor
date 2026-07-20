package com.paymentprocessor.merchantservice.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Preview the fee that would be charged for a transaction of the given amount. */
public record FeePreviewRequest(
        @NotNull @DecimalMin("0.0") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency
) {}
