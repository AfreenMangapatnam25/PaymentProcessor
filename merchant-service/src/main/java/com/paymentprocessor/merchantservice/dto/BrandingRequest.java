package com.paymentprocessor.merchantservice.dto;

import jakarta.validation.constraints.*;

public record BrandingRequest(
        @Size(max = 1024) String logoUrl,
        @Pattern(regexp = "^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$", message = "primaryColor must be a hex color") String primaryColor,
        @Pattern(regexp = "^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$", message = "secondaryColor must be a hex color") String secondaryColor,
        @Size(max = 22) String statementDescriptor,
        @Size(max = 255) String customDomain,
        @Size(max = 255) String emailTemplateRef
) {}
