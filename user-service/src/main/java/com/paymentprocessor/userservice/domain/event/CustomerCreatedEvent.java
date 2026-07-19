package com.paymentprocessor.userservice.domain.event;

import java.time.Instant;

/**
 * Emitted when a merchant-scoped customer is created. Carries ids and status
 * only -- no PII.
 */
public record CustomerCreatedEvent(
        String customerId,
        String merchantId,
        String status,
        Instant occurredAt
) implements DomainEvent {

    public static final String TYPE = "customer.created.v1";

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
