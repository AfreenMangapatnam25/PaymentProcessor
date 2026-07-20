package com.paymentprocessor.ledgerservice.service;

import com.paymentprocessor.ledgerservice.entity.OutboxEvent;
import com.paymentprocessor.ledgerservice.event.OutboxMessage;
import com.paymentprocessor.ledgerservice.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relays outbox records to downstream consumers. Runs on a fixed schedule,
 * claims a batch of unpublished events, emits an in-process {@link OutboxMessage}
 * (which a broker adapter can forward), and marks them published — giving
 * at-least-once delivery semantics.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final int batchSize;

    public OutboxPublisher(OutboxEventRepository repository,
                           ApplicationEventPublisher eventPublisher,
                           Clock clock,
                           @org.springframework.beans.factory.annotation.Value("${ledger.outbox.batch-size:100}") int batchSize) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${ledger.outbox.poll-interval-ms:2000}")
    @Transactional
    public void relayPending() {
        List<OutboxEvent> pending =
                repository.findByPublishedFalseOrderByCreatedAtAsc(PageRequest.of(0, batchSize));
        if (pending.isEmpty()) {
            return;
        }
        Instant now = Instant.now(clock);
        for (OutboxEvent event : pending) {
            try {
                eventPublisher.publishEvent(new OutboxMessage(
                        event.getId(), event.getAggregateType(), event.getAggregateId(),
                        event.getEventType(), event.getPayload()));
                event.setPublished(true);
                event.setPublishedAt(now);
                log.debug("Relayed outbox event {} type={} aggregate={}/{}",
                        event.getId(), event.getEventType(), event.getAggregateType(), event.getAggregateId());
            } catch (RuntimeException ex) {
                // Leave unpublished so it is retried on the next poll.
                log.warn("Failed to relay outbox event {}: {}", event.getId(), ex.getMessage());
            }
        }
        repository.saveAll(pending);
    }
}
