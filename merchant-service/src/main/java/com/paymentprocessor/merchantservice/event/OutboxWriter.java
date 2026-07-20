package com.paymentprocessor.merchantservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.merchantservice.common.enums.DomainEventType;
import com.paymentprocessor.merchantservice.event.outbox.OutboxEvent;
import com.paymentprocessor.merchantservice.event.outbox.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Appends domain events to the transactional outbox. MUST be called inside the same transaction
 * as the state change that produced the event, so that the event and the change commit atomically.
 */
@Component
public class OutboxWriter {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final String topicPrefix;

    public OutboxWriter(OutboxEventRepository outboxRepository, ObjectMapper objectMapper,
                        @Value("${merchant-service.events.topic-prefix}") String topicPrefix) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.topicPrefix = topicPrefix;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(DomainEventType type, String aggregateType, UUID aggregateId, Map<String, Object> data) {
        DomainEvent event = DomainEvent.of(type.getEventName(), aggregateType, aggregateId, data);
        OutboxEvent row = new OutboxEvent();
        row.setAggregateType(aggregateType);
        row.setAggregateId(aggregateId);
        row.setEventType(type.getEventName());
        row.setTopic(topicPrefix + ".events");
        row.setMessageKey(aggregateId.toString());
        row.setPayload(serialize(event));
        outboxRepository.save(row);
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize domain event", e);
        }
    }
}
