package com.paymentprocessor.ledgerservice.event;

/**
 * In-process Spring event emitted by the outbox publisher for each relayed
 * record. Downstream infrastructure adapters (Kafka, SNS, etc.) can subscribe
 * to this to forward events onto a broker.
 */
public record OutboxMessage(
        String id,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload
) {
}
