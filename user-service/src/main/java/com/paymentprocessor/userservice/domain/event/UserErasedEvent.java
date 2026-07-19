package com.paymentprocessor.userservice.domain.event;

import java.time.Instant;

/**
 * Emitted when a user has been GDPR-erased (DEK destroyed, PII unrecoverable).
 * Downstreams use this to purge their own local copies of the subject's PII.
 */
public record UserErasedEvent(
        String userId,
        Instant erasedAt,
        Instant occurredAt
) implements DomainEvent {

    public static final String TYPE = "user.erased.v1";

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
