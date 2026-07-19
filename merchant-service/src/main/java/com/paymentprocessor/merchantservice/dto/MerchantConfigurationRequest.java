package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.CaptureMode;
import com.paymentprocessor.merchantservice.common.enums.FraudScreeningLevel;
import com.paymentprocessor.merchantservice.common.enums.PayoutSchedule;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Full configuration update; null fields fall back to existing values in the service. */
public record MerchantConfigurationRequest(
        CaptureMode captureMode,
        Boolean enforce3ds,
        @Min(0) Integer velocityLimitPerDay,
        PayoutSchedule payoutSchedule,
        @DecimalMin("0.0") BigDecimal minimumPayoutThreshold,
        Boolean instantPayoutEligible,
        FraudScreeningLevel fraudScreeningLevel,
        @Min(0) Integer chargebackAlertThreshold,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal reservePercentage,
        Boolean notifyOnPayout,
        Boolean notifyOnChargeback,
        Boolean notifyOnStatusChange,
        @Min(1) Integer kycRefreshIntervalDays
) {}
