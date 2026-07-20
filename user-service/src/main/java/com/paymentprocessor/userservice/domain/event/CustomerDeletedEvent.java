package com.paymentprocessor.userservice.domain.event;

import java.time.Instant;

/**
 * Emitted when a customer is soft-deleted.
 */
public record CustomerDeletedEvent(
        String customerId,
        String merchantId,
        Instant deletedAt,
        Instant occurredAt
) implements DomainEvent {

    public static final String TYPE = "customer.deleted.v1";

    @Override
    public String aggregateId() {
        return customerId;
    }

    @Override
    public String aggregateType() {
        return "Customer";
    }

    @Override
    public String eventType() {
        return TYPE;
    }
}
