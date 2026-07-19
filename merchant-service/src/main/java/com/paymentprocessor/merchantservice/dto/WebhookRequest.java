package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.WebhookEventType;
import jakarta.validation.constraints.*;
import java.util.Set;

public record WebhookRequest(
        @NotBlank @Pattern(regexp = "^https://.*", message = "endpointUrl must be an https URL")
        @Size(max = 1024) String endpointUrl,
        @NotEmpty Set<WebhookEventType> events,
        @Min(0) @Max(10) Integer maxRetries,
        @Min(1) @Max(60) Integer timeoutSeconds,
        boolean active
) {}
