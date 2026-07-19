package com.paymentprocessor.userservice.domain.event;

import java.time.Instant;

/**
 * Emitted when a user's lifecycle status changes (activate/suspend/lock).
 */
public record UserStatusChangedEvent(
        String userId,
        String previousStatus,
        String newStatus,
        Instant occurredAt
) implements DomainEvent {

    public static final String TYPE = "user.status_changed.v1";

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
