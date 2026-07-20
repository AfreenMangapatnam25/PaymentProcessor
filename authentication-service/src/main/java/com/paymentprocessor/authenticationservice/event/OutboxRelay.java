package com.paymentprocessor.authenticationservice.event;

import com.paymentprocessor.authenticationservice.entity.OutboxEvent;
import com.paymentprocessor.authenticationservice.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Relays staged outbox events to Kafka with at-least-once delivery. Runs on a
 * fixed schedule; each poll claims a batch with {@code FOR UPDATE SKIP LOCKED} so
 * multiple instances cooperate safely. Downstream consumers must be idempotent
 * (every event carries a unique {@code eventId}).
 */
@Component
@ConditionalOnProperty(name = "auth.outbox.relay-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final int batchSize;
    private final int maxAttempts;

    public OutboxRelay(OutboxEventRepository repository,
                       KafkaTemplate<String, String> outboxKafkaTemplate,
                       com.paymentprocessor.authenticationservice.config.AuthProperties props) {
        this.repository = repository;
        this.kafka = outboxKafkaTemplate;
        this.batchSize = props.getOutbox().getBatchSize();
        this.maxAttempts = props.getOutbox().getMaxAttempts();
    }

    @Scheduled(fixedDelayString = "${auth.outbox.poll-interval-ms:2000}")
    @Transactional
    public void flush() {
        List<OutboxEvent> batch = repository.lockPendingBatch(batchSize);
        for (OutboxEvent event : batch) {
            try {
                kafka.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.setStatus(OutboxEvent.Status.PUBLISHED);
                event.setPublishedAt(Instant.now());
                event.setLastError(null);
            } catch (Exception e) {
                int attempts = event.getAttempts() + 1;
                event.setAttempts(attempts);
                event.setLastError(truncate(e.getMessage()));
                if (attempts >= maxAttempts) {
                    event.setStatus(OutboxEvent.Status.FAILED);
                    log.error("Outbox event {} moved to FAILED after {} attempts", event.getId(), attempts);
                } else {
                    log.warn("Outbox delivery failed for {} (attempt {}): {}",
                            event.getId(), attempts, e.getMessage());
                }
            }
        }
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() <= 500 ? s : s.substring(0, 500);
    }
}
