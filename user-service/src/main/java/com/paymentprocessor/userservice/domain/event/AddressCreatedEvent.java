package com.paymentprocessor.userservice.domain.event;

import java.time.Instant;

/**
 * Emitted when an address is created. Carries ids and country (non-PII) only --
 * street lines never travel on an event.
 */
public record AddressCreatedEvent(
        String addressId,
        String ownerType,
        String ownerId,
        String addressType,
        String countryCode,
        Instant occurredAt
) implements DomainEvent {

    public static final String TYPE = "address.created.v1";

    @Override
    public String aggregateId() {
        return addressId;
    }

    @Override
    public String aggregateType() {
        return "Address";
    }

    @Override
    public String eventType() {
        return TYPE;
    }
}
