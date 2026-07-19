package com.paymentprocessor.userservice.domain.event;

import java.time.Instant;

/**
 * Emitted when an address is updated (lines, type, or default flag).
 */
public record AddressUpdatedEvent(
        String addressId,
        String ownerType,
        String ownerId,
        String addressType,
        String countryCode,
        Instant occurredAt
) implements DomainEvent {

    public static final String TYPE = "address.updated.v1";

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
