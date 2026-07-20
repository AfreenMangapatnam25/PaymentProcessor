package com.paymentprocessor.merchantservice.dto;

/** Returned once on key generation; {@code secret} is hashed server-side and never shown again. */
public record ApiKeyCreatedResponse(
        ApiKeyResponse apiKey,
        String secret
) {}
