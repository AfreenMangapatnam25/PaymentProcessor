package com.paymentprocessor.reconciliationservice.event;

import com.paymentprocessor.reconciliationservice.domain.ExceptionRecord;
import com.paymentprocessor.reconciliationservice.domain.ReconRun;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Fallback publisher used when Kafka is disabled ({@code kafka.enabled=false}). Logs only. */
@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "false")
public class NoOpReconEventPublisher implements ReconEventPublisher {

    @Override
    public void publishReconciliationCompleted(ReconRun run) {
        log.info("[events-disabled] ReconciliationCompleted run={} status={}", run.getUuid(), run.getStatus());
    }

    @Override
    public void publishMismatchDetected(ExceptionRecord exception) {
        log.info("[events-disabled] MismatchDetected exception={} category={}",
                exception.getUuid(), exception.getCategory());
    }
}
