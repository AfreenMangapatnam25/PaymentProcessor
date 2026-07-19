package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.PricingPlan;
import java.math.BigDecimal;
import java.util.UUID;

public record FeeConfigurationResponse(
        UUID id, UUID merchantId, PricingPlan pricingPlan, BigDecimal transactionFeePercent,
        BigDecimal transactionFeeFixed, boolean interchangePassThrough, BigDecimal monthlyPlatformFee,
        BigDecimal chargebackFee, BigDecimal refundFee, BigDecimal payoutFee, String currency, boolean active
) {}
