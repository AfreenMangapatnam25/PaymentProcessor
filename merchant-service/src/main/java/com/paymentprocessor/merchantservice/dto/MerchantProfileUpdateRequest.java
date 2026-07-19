package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.BusinessType;
import jakarta.validation.constraints.*;

/** Full replacement of the mutable merchant profile fields. */
public record MerchantProfileUpdateRequest(
        @NotBlank @Size(max = 255) String legalBusinessName,
        @Size(max = 255) String tradingName,
        @Size(max = 100) String registrationNumber,
        @Size(max = 100) String taxId,
        @Pattern(regexp = "\\d{4}", message = "mcc must be a 4-digit code") String mcc,
        @NotNull BusinessType businessType,
        @Size(max = 512) String websiteUrl,
        @Email @Size(max = 255) String supportEmail,
        @Size(max = 40) String supportPhone
) {}
