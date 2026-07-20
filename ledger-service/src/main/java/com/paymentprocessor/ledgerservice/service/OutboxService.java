package com.paymentprocessor.ledgerservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.ledgerservice.entity.OutboxEvent;
import com.paymentprocessor.ledgerservice.repository.OutboxEventRepository;
import com.paymentprocessor.ledgerservice.support.Ids;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Appends domain events to the transactional outbox. Because the write happens
 * in the same transaction as the ledger change, either both commit or neither
 * does — eliminating dual-write inconsistency.
 */
@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** Serialise {@code payload} to JSON and enqueue an outbox record. */
    public OutboxEvent append(String aggregateType, String aggregateId, String eventType, Object payload) {
        OutboxEvent event = new OutboxEvent();
        event.setId(Ids.outboxId());
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(toJson(payload));
        event.setCreatedAt(Instant.now(clock));
        event.setPublished(false);
        return repository.save(event);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise outbox payload", e);
        }
    }
}
