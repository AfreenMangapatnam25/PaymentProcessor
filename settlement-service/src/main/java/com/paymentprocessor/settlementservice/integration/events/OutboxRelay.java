package com.paymentprocessor.settlementservice.integration.events;

import com.paymentprocessor.settlementservice.entity.OutboxEvent;
import com.paymentprocessor.settlementservice.enums.OutboxStatus;
import com.paymentprocessor.settlementservice.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relays pending outbox events to the {@link MessageBus}. Runs on a fixed delay
 * and marks each event PUBLISHED once delivered, guaranteeing at-least-once
 * delivery. A dead-letter cap prevents an unpublishable event from blocking the
 * queue forever.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS = 10;

    private final OutboxEventRepository outboxRepository;
    private final MessageBus messageBus;

    public OutboxRelay(OutboxEventRepository outboxRepository, MessageBus messageBus) {
        this.outboxRepository = outboxRepository;
        this.messageBus = messageBus;
    }

    @Scheduled(fixedDelayString = "${settlement.scheduler.outbox-relay-delay-ms:2000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> pending = outboxRepository.findByStatusOrderByIdAsc(
                OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE));
        for (OutboxEvent event : pending) {
            try {
                messageBus.send(event.getEventType(), event.getAggregateType(),
                        event.getAggregateId(), event.getPayload());
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
            } catch (RuntimeException ex) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(ex.getMessage());
                if (event.getAttempts() >= MAX_ATTEMPTS) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox event {} dead-lettered after {} attempts",
                            event.getId(), event.getAttempts(), ex);
                } else {
                    log.warn("Failed to publish outbox event {} (attempt {})",
                            event.getId(), event.getAttempts(), ex);
                }
            }
        }
    }
}
