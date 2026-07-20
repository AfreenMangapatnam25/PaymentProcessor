package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.PricingPlan;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record FeeConfigurationRequest(
        @NotNull PricingPlan pricingPlan,
        @DecimalMin("0.0") BigDecimal transactionFeePercent,
        @DecimalMin("0.0") BigDecimal transactionFeeFixed,
        boolean interchangePassThrough,
        @DecimalMin("0.0") BigDecimal monthlyPlatformFee,
        @DecimalMin("0.0") BigDecimal chargebackFee,
        @DecimalMin("0.0") BigDecimal refundFee,
        @DecimalMin("0.0") BigDecimal payoutFee,
        @NotBlank @Size(min = 3, max = 3) String currency
) {}
