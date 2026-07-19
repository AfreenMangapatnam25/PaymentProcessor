package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.AddressType;
import jakarta.validation.constraints.*;

public record AddressRequest(
        @NotNull AddressType addressType,
        @NotBlank @Size(max = 255) String line1,
        @Size(max = 255) String line2,
        @NotBlank @Size(max = 120) String city,
        @Size(max = 120) String region,
        @Size(max = 20) String postalCode,
        @NotBlank @Size(min = 2, max = 2) String country,
        boolean primary
) {}
