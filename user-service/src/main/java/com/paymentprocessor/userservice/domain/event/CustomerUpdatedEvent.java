package com.paymentprocessor.userservice.domain.event;

import java.time.Instant;

/**
 * Emitted when a customer's mutable state changes (contact, metadata, status,
 * instrument, user link). Ids/status only -- no PII.
 */
public record CustomerUpdatedEvent(
        String customerId,
        String merchantId,
        String status,
        Instant occurredAt
) implements DomainEvent {

    public static final String TYPE = "customer.updated.v1";

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
