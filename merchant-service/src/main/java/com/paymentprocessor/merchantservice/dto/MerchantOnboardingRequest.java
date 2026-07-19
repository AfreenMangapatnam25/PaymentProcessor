package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.BusinessType;
import jakarta.validation.constraints.*;
import java.util.UUID;

/** Payload to submit a new merchant application (onboarding step 1). */
public record MerchantOnboardingRequest(
        @NotBlank @Size(max = 255) String legalBusinessName,
        @Size(max = 255) String tradingName,
        @Size(max = 100) String registrationNumber,
        @Size(max = 100) String taxId,
        @Pattern(regexp = "\\d{4}", message = "mcc must be a 4-digit code") String mcc,
        @NotNull BusinessType businessType,
        @Size(max = 512) @Pattern(regexp = "^(https?://).*", message = "websiteUrl must be a valid http(s) URL") String websiteUrl,
        @Email @Size(max = 255) String supportEmail,
        @Size(max = 40) String supportPhone,
        @NotBlank @Size(min = 2, max = 2) String country,
        @NotBlank @Size(min = 3, max = 3) String defaultCurrency,
        UUID ownerUserId
) {}
