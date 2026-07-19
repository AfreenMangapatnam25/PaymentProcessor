package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.BusinessType;
import jakarta.validation.constraints.*;

/** Partial update; only non-null fields are applied. */
public record MerchantProfilePatchRequest(
        @Size(max = 255) String legalBusinessName,
        @Size(max = 255) String tradingName,
        @Size(max = 100) String registrationNumber,
        @Size(max = 100) String taxId,
        @Pattern(regexp = "\\d{4}", message = "mcc must be a 4-digit code") String mcc,
        BusinessType businessType,
        @Size(max = 512) String websiteUrl,
        @Email @Size(max = 255) String supportEmail,
        @Size(max = 40) String supportPhone
) {}
