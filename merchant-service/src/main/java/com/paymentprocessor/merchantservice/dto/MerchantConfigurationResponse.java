package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.CaptureMode;
import com.paymentprocessor.merchantservice.common.enums.FraudScreeningLevel;
import com.paymentprocessor.merchantservice.common.enums.PayoutSchedule;
import java.math.BigDecimal;
import java.util.UUID;

public record MerchantConfigurationResponse(
        UUID merchantId, CaptureMode captureMode, boolean enforce3ds, Integer velocityLimitPerDay,
        PayoutSchedule payoutSchedule, BigDecimal minimumPayoutThreshold, boolean instantPayoutEligible,
        FraudScreeningLevel fraudScreeningLevel, Integer chargebackAlertThreshold, BigDecimal reservePercentage,
        boolean notifyOnPayout, boolean notifyOnChargeback, boolean notifyOnStatusChange, Integer kycRefreshIntervalDays
) {}
