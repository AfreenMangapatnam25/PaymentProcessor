package com.paymentprocessor.disputeservice.scheduler;

import com.paymentprocessor.disputeservice.service.DeadlineService;
import com.paymentprocessor.disputeservice.service.DeadlineService.SweepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically runs the deadline sweep to auto-lose overdue disputes and remind
 * merchants about approaching deadlines. The interval is configurable via
 * {@code dispute.deadline.sweep-interval-ms} (default hourly).
 */
@Component
public class DeadlineMonitorJob {

    private static final Logger log = LoggerFactory.getLogger(DeadlineMonitorJob.class);

    private final DeadlineService deadlineService;

    public DeadlineMonitorJob(DeadlineService deadlineService) {
        this.deadlineService = deadlineService;
    }

    @Scheduled(
            fixedDelayString = "${dispute.deadline.sweep-interval-ms:3600000}",
            initialDelayString = "${dispute.deadline.sweep-initial-delay-ms:60000}")
    public void run() {
        try {
            SweepResult result = deadlineService.sweep();
            if (result.autoLost() > 0 || result.remindersSent() > 0) {
                log.info("Deadline sweep: {} auto-lost, {} reminders sent",
                        result.autoLost(), result.remindersSent());
            }
        } catch (Exception ex) {
            log.error("Deadline sweep failed: {}", ex.getMessage(), ex);
        }
    }
}
