package com.paymentprocessor.authenticationservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.entity.OutboxEvent;
import com.paymentprocessor.authenticationservice.event.AuthEvents.*;
import com.paymentprocessor.authenticationservice.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Transactional-outbox implementation of {@link DomainEventPublisher}. Each call
 * stages the event as a row in {@code outbox_events} using the CURRENT transaction
 * (the one the business change runs in), so the event and the state change commit
 * atomically. The {@link OutboxRelay} forwards staged rows to Kafka.
 *
 * <p>This deliberately does NOT touch Kafka, and is NOT {@code @Async}: it must
 * enlist in the caller's JPA transaction to be atomic.</p>
 */
@Component
public class OutboxEventPublisher implements DomainEventPublisher {

    private static final String AGGREGATE = "identity";

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final AuthProperties.Events.Topics topics;

    public OutboxEventPublisher(OutboxEventRepository repository,
                                ObjectMapper objectMapper,
                                AuthProperties props) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.topics = props.getEvents().getTopics();
    }

    @Override
    public void publish(UserLoggedIn event) {
        stage(event.identityId(), "UserLoggedIn", topics.getUserLoggedIn(), event);
    }

    @Override
    public void publish(UserLoggedOut event) {
        stage(event.identityId(), "UserLoggedOut", topics.getUserLoggedOut(), event);
    }

    @Override
    public void publish(PasswordChanged event) {
        stage(event.identityId(), "PasswordChanged", topics.getPasswordChanged(), event);
    }

    @Override
    public void publish(MfaEnabled event) {
        stage(event.identityId(), "MFAEnabled", topics.getMfaEnabled(), event);
    }

    @Override
    public void publish(AccountLocked event) {
        stage(event.identityId(), "AccountLocked", topics.getAccountLocked(), event);
    }

    @Override
    public void publish(NewDeviceLogin event) {
        stage(event.identityId(), "NewDeviceLogin", topics.getNewDevice(), event);
    }

    private void stage(String aggregateId, String eventType, String topic, Object payload) {
        if (topic == null || topic.isBlank()) {
            return;
        }
        OutboxEvent row = new OutboxEvent();
        row.setId(UUID.randomUUID().toString());
        row.setAggregateType(AGGREGATE);
        row.setAggregateId(aggregateId);
        row.setEventType(eventType);
        row.setTopic(topic);
        row.setPayload(serialize(payload));
        row.setStatus(OutboxEvent.Status.PENDING);
        repository.save(row);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
