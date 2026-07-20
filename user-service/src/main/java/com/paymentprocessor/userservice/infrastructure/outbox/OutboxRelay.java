package com.paymentprocessor.userservice.infrastructure.outbox;

import com.paymentprocessor.userservice.common.util.ClockProvider;
import com.paymentprocessor.userservice.infrastructure.config.AppProperties;
import com.paymentprocessor.userservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.paymentprocessor.userservice.infrastructure.persistence.repository.OutboxJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Drains the transactional outbox to Kafka. Runs on every node; concurrent
 * relays are safe because due rows are claimed with SKIP LOCKED. Each poll runs
 * in one transaction: claim a batch, publish, and update status. On failure the
 * row is retried with exponential backoff up to {@code max-attempts}, then
 * parked as FAILED for operator attention.
 *
 * <p>Enabled by {@code app.outbox.relay.enabled} (default true). Publishing
 * inside the claiming transaction keeps the row locked until the broker acks;
 * the trade-off is a longer transaction, bounded by the batch size and a send
 * timeout.
 */
@Component
@ConditionalOnProperty(name = "app.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class OutboxRelay {

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_FAILED = "FAILED";
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

    private final OutboxJpaRepository repository;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final ClockProvider clock;
    private final int batchSize;
    private final int maxAttempts;

    public OutboxRelay(OutboxJpaRepository repository,
                       KafkaTemplate<String, byte[]> kafkaTemplate,
                       ClockProvider clock,
                       AppProperties properties) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
        AppProperties.Outbox.Relay relay = properties.outbox().relay();
        this.batchSize = relay.batchSize();
        this.maxAttempts = relay.maxAttempts();
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay.poll-interval-ms:1000}")
    @Transactional
    public void drain() {
        List<OutboxEventEntity> due =
                repository.findDueForUpdate(clock.now(), PageRequest.of(0, batchSize));
        for (OutboxEventEntity event : due) {
            publish(event);
        }
    }

    private void publish(OutboxEventEntity event) {
        try {
            kafkaTemplate.send(
                    event.getTopic(),
                    event.getPartitionKey(),
                    event.getPayload().getBytes(StandardCharsets.UTF_8)
            ).get(SEND_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

            event.setStatus(STATUS_PUBLISHED);
            event.setPublishedAt(clock.now());
            event.setLastError(null);
        } catch (Exception e) {
            int attempts = event.getAttempts() + 1;
            event.setAttempts(attempts);
            event.setLastError(truncate(e.getMessage()));
            if (attempts >= maxAttempts) {
                event.setStatus(STATUS_FAILED);
                log.error("Outbox event {} parked as FAILED after {} attempts", event.getId(), attempts);
            } else {
                Instant backoff = clock.now().plusSeconds((long) Math.pow(2, Math.min(attempts, 6)));
                event.setNextAttemptAt(backoff);
                log.warn("Outbox event {} publish failed (attempt {}), retrying after {}",
                        event.getId(), attempts, backoff);
            }
        }
    }

    private static String truncate(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() > 1000 ? msg.substring(0, 1000) : msg;
    }
}
