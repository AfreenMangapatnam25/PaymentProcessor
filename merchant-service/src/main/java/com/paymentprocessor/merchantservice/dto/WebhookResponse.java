package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.WebhookEventType;
import java.util.Set;
import java.util.UUID;

public record WebhookResponse(
        UUID id, UUID merchantId, String endpointUrl, Set<WebhookEventType> events,
        boolean active, int maxRetries, int timeoutSeconds
) {}
