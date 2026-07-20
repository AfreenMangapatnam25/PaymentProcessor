package com.paymentprocessor.userservice.domain.event;

import java.time.Instant;

/**
 * Emitted when consent is revoked for a (subject, kind). No PII.
 */
public record ConsentRevokedEvent(
        String consentId,
        String subjectType,
        String subjectId,
        String kind,
        Instant occurredAt
) implements DomainEvent {

    public static final String TYPE = "consent.revoked.v1";

    @Override
    public String aggregateId() {
        return consentId;
    }

    @Override
    public String aggregateType() {
        return "Consent";
    }

    @Override
    public String eventType() {
        return TYPE;
    }
}
