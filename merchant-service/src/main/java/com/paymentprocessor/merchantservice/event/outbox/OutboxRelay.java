package com.paymentprocessor.merchantservice.event.outbox;

import com.paymentprocessor.merchantservice.common.enums.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Relays pending outbox rows to Kafka on a fixed schedule (at-least-once). Successful sends mark
 * the row PUBLISHED; failures increment the attempt count and are retried on the next poll.
 * Consumers must be idempotent (each event carries a unique eventId).
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;

    public OutboxRelay(OutboxEventRepository outboxRepository,
                       KafkaTemplate<String, String> kafkaTemplate,
                       @org.springframework.beans.factory.annotation.Value("${merchant-service.outbox.batch-size}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${merchant-service.outbox.poll-interval-ms}")
    @Transactional
    public void relayPending() {
        List<OutboxEvent> pending = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, batchSize));
        if (pending.isEmpty()) {
            return;
        }
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .get();  // block within the poll to confirm the broker ack
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                event.setLastError(null);
            } catch (Exception e) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(truncate(e.getMessage()));
                log.warn("Failed to relay outbox event {} (attempt {}): {}",
                        event.getId(), event.getAttempts(), e.getMessage());
            }
        }
        outboxRepository.saveAll(pending);
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 2000 ? s.substring(0, 2000) : s;
    }
}
