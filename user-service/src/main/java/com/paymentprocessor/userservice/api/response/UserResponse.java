package com.paymentprocessor.userservice.api.response;

import java.time.Instant;

/**
 * User projection returned by the API. {@code version} doubles as the ETag the
 * client echoes back for optimistic-locked writes.
 */
public record UserResponse(
        String id,
        String identityId,
        String status,
        UserProfileResponse profile,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Instant erasedAt
) {
}
