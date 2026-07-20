package com.paymentprocessor.userservice.domain.event;

import java.time.Instant;

/**
 * Emitted when an address is soft-deleted.
 */
public record AddressDeletedEvent(
        String addressId,
        String ownerType,
        String ownerId,
        Instant deletedAt,
        Instant occurredAt
) implements DomainEvent {

    public static final String TYPE = "address.deleted.v1";

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
