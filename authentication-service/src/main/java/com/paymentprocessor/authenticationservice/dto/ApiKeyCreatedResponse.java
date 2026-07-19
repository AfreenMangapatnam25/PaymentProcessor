package com.paymentprocessor.authenticationservice.dto;

/** Returned once on creation. {@code secret} is the only time the plaintext key is exposed. */
public record ApiKeyCreatedResponse(ApiKeyResponse key, String secret) {
}
