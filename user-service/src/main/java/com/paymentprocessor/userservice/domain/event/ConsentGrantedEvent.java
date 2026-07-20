package com.paymentprocessor.userservice.domain.event;

import java.time.Instant;

/**
 * Emitted when consent is granted for a (subject, kind). No PII.
 */
public record ConsentGrantedEvent(
        String consentId,
        String subjectType,
        String subjectId,
        String kind,
        String policyVersion,
        Instant occurredAt
) implements DomainEvent {

    public static final String TYPE = "consent.granted.v1";

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
