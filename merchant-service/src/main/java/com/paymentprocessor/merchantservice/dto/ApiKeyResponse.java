package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.ApiKeyStatus;
import com.paymentprocessor.merchantservice.common.enums.ApiKeyType;
import java.time.Instant;
import java.util.UUID;

public record ApiKeyResponse(
        UUID id, UUID merchantId, String keyId, ApiKeyType keyType, ApiKeyStatus status,
        String label, String ipAllowlist, Instant lastUsedAt, Instant expiresAt, Instant createdAt
) {}
