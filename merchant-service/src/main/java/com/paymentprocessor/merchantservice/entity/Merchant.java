package com.paymentprocessor.merchantservice.entity;

import com.paymentprocessor.merchantservice.common.enums.BusinessType;
import com.paymentprocessor.merchantservice.common.enums.KybStatus;
import com.paymentprocessor.merchantservice.common.enums.MerchantStatus;
import com.paymentprocessor.merchantservice.common.enums.PricingPlan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Core merchant aggregate — the system-of-record identity, profile, compliance status,
 * and lifecycle state for a business on the platform.
 */
@Entity
@Table(name = "merchant", indexes = {
        @Index(name = "ux_merchant_reference", columnList = "merchant_reference", unique = true),
        @Index(name = "ix_merchant_status", columnList = "status"),
        @Index(name = "ix_merchant_owner_user", columnList = "owner_user_id")
})
@Getter
@Setter
public class Merchant extends BaseEntity {

    /** Public, opaque business identifier (e.g. "mch_a1b2c3"). Referenced by other services. */
    @Column(name = "merchant_reference", nullable = false, updatable = false, length = 40)
    private String merchantReference;

    @Column(name = "legal_business_name", nullable = false, length = 255)
    private String legalBusinessName;

    @Column(name = "trading_name", length = 255)
    private String tradingName;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    /** Merchant Category Code. */
    @Column(name = "mcc", length = 4)
    private String mcc;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", length = 40)
    private BusinessType businessType;

    @Column(name = "website_url", length = 512)
    private String websiteUrl;

    @Column(name = "support_email", length = 255)
    private String supportEmail;

    @Column(name = "support_phone", length = 40)
    private String supportPhone;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "default_currency", length = 3)
    private String defaultCurrency;

    /** Reference to the owning platform user (User Service). */
    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MerchantStatus status = MerchantStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyb_status", nullable = false, length = 20)
    private KybStatus kybStatus = KybStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_plan", nullable = false, length = 20)
    private PricingPlan pricingPlan = PricingPlan.STANDARD;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspension_reason", length = 512)
    private String suspensionReason;

    @Column(name = "terminated_at")
    private Instant terminatedAt;
}
