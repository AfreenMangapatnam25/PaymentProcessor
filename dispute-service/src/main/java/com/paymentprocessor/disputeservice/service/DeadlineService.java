package com.paymentprocessor.disputeservice.service;

import com.paymentprocessor.disputeservice.domain.enums.NotificationChannel;
import com.paymentprocessor.disputeservice.domain.enums.NotificationUrgency;
import com.paymentprocessor.disputeservice.domain.enums.TimelineEventType;
import com.paymentprocessor.disputeservice.entity.Dispute;
import com.paymentprocessor.disputeservice.integration.NotificationClient;
import com.paymentprocessor.disputeservice.repository.DisputeRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enforces network response deadlines. Auto-loses disputes whose deadline has
 * passed and sends escalating reminders (T-3, T-1) for those approaching one, so
 * the platform never suffers an automatic loss through inaction.
 */
@Service
public class DeadlineService {

    private static final Logger log = LoggerFactory.getLogger(DeadlineService.class);

    /** Reminder threshold, in days, that triggers a critical alert. */
    private static final long CRITICAL_DAYS = 1;

    private final DisputeRepository disputeRepository;
    private final DisputeService disputeService;
    private final DisputeEventService timeline;
    private final NotificationClient notificationClient;

    public DeadlineService(DisputeRepository disputeRepository, DisputeService disputeService,
                           DisputeEventService timeline, NotificationClient notificationClient) {
        this.disputeRepository = disputeRepository;
        this.disputeService = disputeService;
        this.timeline = timeline;
        this.notificationClient = notificationClient;
    }

    /**
     * One deadline-monitoring pass: auto-loses overdue disputes and reminds on
     * those approaching their deadline.
     *
     * @return a summary of the actions taken
     */
    @Transactional
    public SweepResult sweep() {
        Instant now = Instant.now();

        List<Dispute> overdue = disputeRepository.findByStatusInAndDeadlineAtBefore(
                DisputeService.AWAITING_RESPONSE, now);
        for (Dispute dispute : overdue) {
            log.warn("Dispute {} missed its deadline {}; auto-losing", dispute.getId(), dispute.getDeadlineAt());
            disputeService.autoLoseOnMissedDeadline(dispute.getId());
        }

        Instant windowEnd = now.plus(3, ChronoUnit.DAYS);
        List<Dispute> approaching = disputeRepository.findByStatusInAndDeadlineAtBetween(
                DisputeService.AWAITING_RESPONSE, now, windowEnd);
        for (Dispute dispute : approaching) {
            remind(dispute, now);
        }

        return new SweepResult(overdue.size(), approaching.size());
    }

    private void remind(Dispute dispute, Instant now) {
        long daysLeft = dispute.daysUntilDeadline(now);
        NotificationUrgency urgency = daysLeft <= CRITICAL_DAYS
                ? NotificationUrgency.CRITICAL : NotificationUrgency.HIGH;
        Set<NotificationChannel> channels = urgency == NotificationUrgency.CRITICAL
                ? Set.of(NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.DASHBOARD)
                : Set.of(NotificationChannel.EMAIL, NotificationChannel.DASHBOARD);

        notificationClient.notifyMerchant(dispute.getMerchantId(), dispute.getId(),
                "Deadline approaching",
                "Dispute " + dispute.getId() + " must be answered within " + Math.max(daysLeft, 0)
                        + " day(s) — by " + dispute.getDeadlineAt(),
                urgency, channels);
        timeline.record(dispute, TimelineEventType.DEADLINE_APPROACHING, DisputeEventService.ACTOR_SYSTEM,
                "Reminder sent: " + Math.max(daysLeft, 0) + " day(s) remaining");
    }

    /**
     * Outcome of a deadline sweep.
     *
     * @param autoLost        number of disputes auto-lost for a missed deadline
     * @param remindersSent   number of approaching-deadline reminders sent
     */
    public record SweepResult(int autoLost, int remindersSent) {
    }
}
