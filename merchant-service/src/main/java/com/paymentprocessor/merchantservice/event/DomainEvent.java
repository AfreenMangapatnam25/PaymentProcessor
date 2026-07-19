package com.paymentprocessor.merchantservice.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Canonical envelope for a domain event published by the Merchant Service. Serialized to JSON
 * and stored in the outbox before relay to Kafka.
 */
public record DomainEvent(
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        Instant occurredAt,
        int schemaVersion,
        Map<String, Object> data
) {
    public static DomainEvent of(String eventType, String aggregateType, UUID aggregateId, Map<String, Object> data) {
        return new DomainEvent(UUID.randomUUID(), eventType, aggregateType, aggregateId,
                Instant.now(), 1, data);
    }
}
