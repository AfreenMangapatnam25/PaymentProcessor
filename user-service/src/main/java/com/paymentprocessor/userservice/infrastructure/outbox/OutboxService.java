package com.paymentprocessor.userservice.infrastructure.outbox;

import com.paymentprocessor.userservice.application.port.out.OutboxPort;
import com.paymentprocessor.userservice.common.id.IdGenerator;
import com.paymentprocessor.userservice.common.util.ClockProvider;
import com.paymentprocessor.userservice.common.util.JsonUtils;
import com.paymentprocessor.userservice.domain.event.DomainEvent;
import com.paymentprocessor.userservice.infrastructure.kafka.KafkaTopics;
import com.paymentprocessor.userservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.paymentprocessor.userservice.infrastructure.persistence.repository.OutboxJpaRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Appends domain events to the outbox. MUST be invoked inside the same
 * transaction as the state change that produced the event (the calling
 * application service is {@code @Transactional}), guaranteeing atomicity
 * between the write and the intent to publish (rule 11).
 */
@Service
public class OutboxService implements OutboxPort {

    private static final String STATUS_PENDING = "PENDING";

    private final OutboxJpaRepository repository;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;

    public OutboxService(OutboxJpaRepository repository, IdGenerator idGenerator, ClockProvider clock) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Override
    public void append(DomainEvent event) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setId(idGenerator.generateOutboxId());
        entity.setAggregateType(event.aggregateType());
        entity.setAggregateId(event.aggregateId());
        entity.setEventType(event.eventType());
        entity.setPayload(JsonUtils.toJson(event));
        entity.setHeaders(JsonUtils.toJson(Map.of("eventType", event.eventType())));
        entity.setTopic(KafkaTopics.forAggregate(event.aggregateType()));
        entity.setPartitionKey(event.aggregateId());
        entity.setStatus(STATUS_PENDING);
        entity.setAttempts(0);
        entity.setNextAttemptAt(clock.now());
        entity.setCreatedAt(clock.now());
        entity.setTraceId(MDC.get("correlationId"));
        repository.save(entity);
    }
}
