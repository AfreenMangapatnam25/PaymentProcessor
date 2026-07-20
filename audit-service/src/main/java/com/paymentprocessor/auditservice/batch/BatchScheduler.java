package com.paymentprocessor.auditservice.batch;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers daily sealing of the previous UTC day. Enabled via {@code audit.batch.enabled}
 * and scheduled by {@code audit.batch.cron} (default 00:30 UTC).
 *
 * <p>Failures are logged but not rethrown so a transient error does not stop the
 * scheduler; {@link DailyBatchService#sealDay} is idempotent, so the next run (or a
 * manual trigger) safely completes the batch.
 */
@Component
@ConditionalOnProperty(prefix = "audit.batch", name = "enabled", havingValue = "true")
public class BatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(BatchScheduler.class);

    private final DailyBatchService batchService;

    public BatchScheduler(DailyBatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(cron = "${audit.batch.cron}", zone = "${audit.batch.zone}")
    public void sealPreviousDay() {
        LocalDate day = batchService.previousDay();
        log.info("Scheduled batch sealing starting for {}", day);
        try {
            batchService.sealDay(day);
        } catch (Exception e) {
            log.error("Scheduled batch sealing for {} failed; will retry on next run", day, e);
        }
    }
}
