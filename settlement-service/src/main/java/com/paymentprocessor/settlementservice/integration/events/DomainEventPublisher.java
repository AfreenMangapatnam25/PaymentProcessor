package com.paymentprocessor.settlementservice.integration.events;

/**
 * Publishes domain events. The default implementation uses the transactional
 * outbox pattern: the event is persisted in the same transaction as the state
 * change, then relayed asynchronously to the message bus.
 */
public interface DomainEventPublisher {

    /**
     * Records a domain event for at-least-once delivery.
     *
     * @param eventType     one of {@link EventTypes}
     * @param aggregateType the aggregate root type (e.g. "SettlementBatch", "Payout")
     * @param aggregateId   the aggregate identifier
     * @param payload       an object serialised to JSON as the event body
     */
    void publish(String eventType, String aggregateType, String aggregateId, Object payload);
}
