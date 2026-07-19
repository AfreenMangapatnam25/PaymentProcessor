package com.paymentprocessor.settlementservice.integration.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.settlementservice.entity.OutboxEvent;
import com.paymentprocessor.settlementservice.enums.OutboxStatus;
import com.paymentprocessor.settlementservice.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;

/**
 * Default {@link DomainEventPublisher} that writes events to the outbox table.
 * Because this runs inside the caller's transaction, the event and the state
 * change commit atomically. A separate {@link OutboxRelay} delivers them.
 */
@Component
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxDomainEventPublisher(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String eventType, String aggregateType, String aggregateId, Object payload) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setPayload(serialize(payload));
        event.setStatus(OutboxStatus.PENDING);
        outboxRepository.save(event);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialise event payload", e);
        }
    }
}
