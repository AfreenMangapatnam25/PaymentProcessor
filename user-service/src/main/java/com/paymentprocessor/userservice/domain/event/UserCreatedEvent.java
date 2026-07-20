package com.paymentprocessor.userservice.domain.event;

import java.time.Instant;

/**
 * Emitted when a platform user is registered. Carries the opaque identity
 * reference and status only -- no name/email/DOB.
 */
public record UserCreatedEvent(
        String userId,
        String identityId,
        String status,
        Instant occurredAt
) implements DomainEvent {

    public static final String TYPE = "user.created.v1";

    @Override
    public String aggregateId() {
        return userId;
    }

    @Override
    public String aggregateType() {
        return "User";
    }

    @Override
    public String eventType() {
        return TYPE;
    }
}
