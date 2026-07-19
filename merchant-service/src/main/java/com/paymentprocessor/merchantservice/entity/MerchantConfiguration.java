package com.paymentprocessor.merchantservice.entity;

import com.paymentprocessor.merchantservice.common.enums.CaptureMode;
import com.paymentprocessor.merchantservice.common.enums.FraudScreeningLevel;
import com.paymentprocessor.merchantservice.common.enums.PayoutSchedule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Centralized operational settings for a merchant (one row per merchant): transaction, payout,
 * risk, notification, and compliance configuration.
 */
@Entity
@Table(name = "merchant_configuration", indexes = {
        @Index(name = "ux_config_merchant", columnList = "merchant_id", unique = true)
})
@Getter
@Setter
public class MerchantConfiguration extends BaseEntity {

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    // --- Transaction ---
    @Enumerated(EnumType.STRING)
    @Column(name = "capture_mode", nullable = false, length = 20)
    private CaptureMode captureMode = CaptureMode.AUTOMATIC;

    @Column(name = "enforce_3ds", nullable = false)
    private boolean enforce3ds = false;

    @Column(name = "velocity_limit_per_day")
    private Integer velocityLimitPerDay;

    // --- Payout ---
    @Enumerated(EnumType.STRING)
    @Column(name = "payout_schedule", nullable = false, length = 20)
    private PayoutSchedule payoutSchedule = PayoutSchedule.DAILY;

    @Column(name = "minimum_payout_threshold", precision = 12, scale = 2)
    private BigDecimal minimumPayoutThreshold;

    @Column(name = "instant_payout_eligible", nullable = false)
    private boolean instantPayoutEligible = false;

    // --- Risk ---
    @Enumerated(EnumType.STRING)
    @Column(name = "fraud_screening_level", nullable = false, length = 10)
    private FraudScreeningLevel fraudScreeningLevel = FraudScreeningLevel.MEDIUM;

    @Column(name = "chargeback_alert_threshold")
    private Integer chargebackAlertThreshold;

    @Column(name = "reserve_percentage", precision = 5, scale = 2)
    private BigDecimal reservePercentage;

    // --- Notification ---
    @Column(name = "notify_on_payout", nullable = false)
    private boolean notifyOnPayout = true;

    @Column(name = "notify_on_chargeback", nullable = false)
    private boolean notifyOnChargeback = true;

    @Column(name = "notify_on_status_change", nullable = false)
    private boolean notifyOnStatusChange = true;

    // --- Compliance ---
    @Column(name = "kyc_refresh_interval_days")
    private Integer kycRefreshIntervalDays;
}
