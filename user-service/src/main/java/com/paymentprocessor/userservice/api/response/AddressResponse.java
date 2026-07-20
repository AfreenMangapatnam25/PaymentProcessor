package com.paymentprocessor.userservice.api.response;

import java.time.Instant;

/**
 * Address projection. Lines are decrypted at the boundary; null if the owner
 * has been erased.
 */
public record AddressResponse(
        String id,
        String ownerType,
        String ownerId,
        String addressType,
        String line1,
        String line2,
        String city,
        String region,
        String postalCode,
        String countryCode,
        boolean defaultAddress,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
}
