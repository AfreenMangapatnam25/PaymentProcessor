package com.paymentprocessor.settlementservice.service;

import com.paymentprocessor.settlementservice.config.SettlementProperties;
import com.paymentprocessor.settlementservice.entity.Payout;
import com.paymentprocessor.settlementservice.enums.ScheduleType;
import com.paymentprocessor.settlementservice.service.initiation.InitiationService;
import com.paymentprocessor.settlementservice.service.retry.RetryService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron-driven entry points for the settlement cycle, the retry sweep, and the
 * reserve-release sweep. Disabled by setting {@code settlement.scheduler.enabled=false}.
 */
@Component
@ConditionalOnProperty(prefix = "settlement.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SettlementScheduler {

    private static final Logger log = LoggerFactory.getLogger(SettlementScheduler.class);

    private final SettlementRunService runService;
    private final RetryService retryService;
    private final InitiationService initiationService;
    private final ReserveService reserveService;

    public SettlementScheduler(SettlementRunService runService,
                               RetryService retryService,
                               InitiationService initiationService,
                               ReserveService reserveService) {
        this.runService = runService;
        this.retryService = retryService;
        this.initiationService = initiationService;
        this.reserveService = reserveService;
    }

    /** Runs the main settlement cycle. */
    @Scheduled(cron = "${settlement.scheduler.cycle-cron}")
    public void runSettlementCycle() {
        log.debug("Scheduled settlement cycle starting");
        runService.runCycle(ScheduleType.DAILY);
    }

    /** Re-submits payouts whose retry backoff has elapsed. */
    @Scheduled(cron = "${settlement.scheduler.retry-cron}")
    public void processRetries() {
        List<Payout> due = retryService.dueRetries(Instant.now());
        for (Payout payout : due) {
            try {
                initiationService.retryPayout(payout);
            } catch (RuntimeException ex) {
                log.error("Retry failed for payout {}: {}", payout.getId(), ex.getMessage(), ex);
            }
        }
        if (!due.isEmpty()) {
            log.info("Processed {} due payout retries", due.size());
        }
    }

    /** Releases reserves whose hold period has elapsed. */
    @Scheduled(cron = "${settlement.scheduler.reserve-release-cron}")
    public void releaseReserves() {
        reserveService.releaseDueReserves(LocalDate.now());
    }
}
