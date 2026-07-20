package com.paymentprocessor.analytics.service.schedule;

import com.paymentprocessor.analytics.config.AnalyticsProperties;
import com.paymentprocessor.analytics.domain.entity.ReportJob;
import com.paymentprocessor.analytics.domain.enums.ReportStatus;
import com.paymentprocessor.analytics.repository.ReportJobRepository;
import com.paymentprocessor.analytics.service.storage.StorageService;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Expires completed reports past their retention window and deletes their stored files. */
@Component
public class RetentionJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

    private final ReportJobRepository jobs;
    private final StorageService storage;

    public RetentionJob(ReportJobRepository jobs, StorageService storage, AnalyticsProperties props) {
        this.jobs = jobs;
        this.storage = storage;
    }

    /** Nightly at 03:15 UTC. */
    @Scheduled(cron = "${analytics.retention.cron:0 15 3 * * *}", zone = "UTC")
    @Transactional
    public void purgeExpired() {
        Instant now = Instant.now();
        List<ReportJob> expiring = jobs.findByStatusAndExpiresAtBefore(ReportStatus.COMPLETED, now);
        int deleted = 0;
        for (ReportJob job : expiring) {
            try {
                if (job.getStorageKey() != null) {
                    storage.delete(job.getStorageKey());
                }
                job.markExpired();
                jobs.save(job);
                deleted++;
            } catch (RuntimeException e) {
                log.error("Failed to purge report {}", job.getId(), e);
            }
        }
        if (deleted > 0) {
            log.info("Retention: expired {} report(s)", deleted);
        }
    }
}
