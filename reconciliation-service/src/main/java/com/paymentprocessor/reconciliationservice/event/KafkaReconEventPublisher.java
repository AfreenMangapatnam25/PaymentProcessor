package com.paymentprocessor.reconciliationservice.event;

import com.paymentprocessor.reconciliationservice.config.ReconProperties;
import com.paymentprocessor.reconciliationservice.domain.ExceptionRecord;
import com.paymentprocessor.reconciliationservice.domain.ReconRun;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-backed event publisher. Active by default; disabled when {@code kafka.enabled=false}, in
 * which case {@link NoOpReconEventPublisher} is used instead and no broker is required.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaReconEventPublisher implements ReconEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ReconProperties properties;

    public KafkaReconEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ReconProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publishReconciliationCompleted(ReconRun run) {
        ReconciliationCompletedEvent event = new ReconciliationCompletedEvent(
                run.getUuid(),
                run.getReconType().name(),
                run.getChannel(),
                String.valueOf(run.getBusinessDate()),
                run.getStatus().name(),
                run.getTotalInternal(),
                run.getTotalExternal(),
                run.getMatchedCount(),
                run.getMismatchedCount(),
                run.getMissingInternalCount(),
                run.getMissingExternalCount(),
                run.getDuplicateCount(),
                run.getExceptionCount(),
                run.getMatchRate(),
                run.getCompletedAt());
        String topic = properties.getEvents().getTopic().getReconciliationCompleted();
        kafkaTemplate.send(topic, run.getUuid().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ReconciliationCompleted for run {}", run.getUuid(), ex);
                    } else {
                        log.debug("Published ReconciliationCompleted for run {}", run.getUuid());
                    }
                });
    }

    @Override
    public void publishMismatchDetected(ExceptionRecord exception) {
        ReconRun run = exception.getReconRun();
        MismatchDetectedEvent event = new MismatchDetectedEvent(
                exception.getUuid(),
                run.getUuid(),
                run.getReconType().name(),
                run.getChannel(),
                exception.getCategory().name(),
                exception.getSeverityLevel().name(),
                exception.getSeverityScore(),
                exception.getAmount(),
                exception.getCurrency(),
                exception.getExternalReference(),
                exception.getReviewQueue() == null ? null : exception.getReviewQueue().name(),
                exception.getDescription(),
                exception.getDetectedAt());
        String topic = properties.getEvents().getTopic().getMismatchDetected();
        kafkaTemplate.send(topic, exception.getUuid().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish MismatchDetected for exception {}", exception.getUuid(), ex);
                    }
                });
    }
}
