package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.BusinessType;
import com.paymentprocessor.merchantservice.common.enums.KybStatus;
import com.paymentprocessor.merchantservice.common.enums.MerchantStatus;
import com.paymentprocessor.merchantservice.common.enums.PricingPlan;
import java.time.Instant;
import java.util.UUID;

public record MerchantResponse(
        UUID id,
        String merchantReference,
        String legalBusinessName,
        String tradingName,
        String registrationNumber,
        String taxId,
        String mcc,
        BusinessType businessType,
        String websiteUrl,
        String supportEmail,
        String supportPhone,
        String country,
        String defaultCurrency,
        UUID ownerUserId,
        MerchantStatus status,
        KybStatus kybStatus,
        PricingPlan pricingPlan,
        Instant activatedAt,
        Instant suspendedAt,
        String suspensionReason,
        Instant createdAt,
        Instant updatedAt
) {}
