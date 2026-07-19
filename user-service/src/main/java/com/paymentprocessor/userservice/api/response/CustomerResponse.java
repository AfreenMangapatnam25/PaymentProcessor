package com.paymentprocessor.userservice.api.response;

import java.time.Instant;
import java.util.Map;

/**
 * Customer projection returned to authorized callers within their merchant
 * scope. PII is decrypted at the boundary; null after erasure.
 */
public record CustomerResponse(
        String id,
        String merchantId,
        String userId,
        String externalRef,
        String email,
        String fullName,
        String phone,
        String defaultInstrumentToken,
        String status,
        Map<String, String> metadata,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        Instant erasedAt
) {
}
