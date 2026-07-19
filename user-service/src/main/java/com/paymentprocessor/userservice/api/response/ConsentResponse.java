package com.paymentprocessor.userservice.api.response;

import java.time.Instant;

/**
 * Consent projection: the current lawful-basis state for a (subject, kind).
 */
public record ConsentResponse(
        String id,
        String subjectType,
        String subjectId,
        String kind,
        boolean granted,
        String source,
        String policyVersion,
        Instant grantedAt,
        Instant revokedAt,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
