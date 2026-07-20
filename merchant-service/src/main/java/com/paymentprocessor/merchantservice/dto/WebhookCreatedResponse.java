package com.paymentprocessor.merchantservice.dto;

/** Returned once on webhook creation; {@code signingSecret} is never retrievable again. */
public record WebhookCreatedResponse(
        WebhookResponse webhook,
        String signingSecret
) {}
