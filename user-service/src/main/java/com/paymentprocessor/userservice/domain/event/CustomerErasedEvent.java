package com.paymentprocessor.userservice.domain.event;

import java.time.Instant;

/**
 * Emitted when a customer has been GDPR-erased (DEK destroyed).
 */
public record CustomerErasedEvent(
        String customerId,
        String merchantId,
        Instant erasedAt,
        Instant occurredAt
) implements DomainEvent {

    public static final String TYPE = "customer.erased.v1";

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
