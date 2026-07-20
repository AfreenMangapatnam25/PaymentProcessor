package com.paymentprocessor.merchantservice.dto;

import java.util.UUID;

public record BrandingResponse(
        UUID merchantId, String logoUrl, String primaryColor, String secondaryColor,
        String statementDescriptor, String customDomain, String emailTemplateRef
) {}
