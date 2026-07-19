package com.paymentprocessor.analytics.service.schedule;

import com.paymentprocessor.analytics.domain.entity.ScheduledReport;
import com.paymentprocessor.analytics.dto.CreateReportRequest;
import com.paymentprocessor.analytics.repository.ScheduledReportRepository;
import com.paymentprocessor.analytics.service.ReportService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls for due scheduled reports and materializes them into async report jobs. Runs on
 * a fixed cadence; each due schedule is advanced to its next fire time so it is not
 * double-dispatched even across instances (row-level update + optimistic version).
 */
@Component
public class ScheduledReportDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ScheduledReportDispatcher.class);
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };

    private final ScheduledReportRepository repo;
    private final ReportService reportService;
    private final ScheduleCalculator calculator;
    private final ObjectMapper objectMapper;

    public ScheduledReportDispatcher(ScheduledReportRepository repo, ReportService reportService,
                                     ScheduleCalculator calculator, ObjectMapper objectMapper) {
        this.repo = repo;
        this.reportService = reportService;
        this.calculator = calculator;
        this.objectMapper = objectMapper;
    }

    /** Every minute, dispatch anything whose next_run_at has passed. */
    @Scheduled(fixedDelayString = "${analytics.schedule.poll-interval-ms:60000}")
    @Transactional
    public void dispatchDue() {
        Instant now = Instant.now();
        var due = repo.findByEnabledTrueAndNextRunAtLessThanEqual(now);
        if (due.isEmpty()) {
            return;
        }
        log.info("Dispatching {} due scheduled report(s)", due.size());
        for (ScheduledReport s : due) {
            try {
                dispatchOne(s, now);
            } catch (RuntimeException e) {
                log.error("Failed to dispatch scheduled report {}", s.getId(), e);
                // still advance next run so a poison schedule doesn't spin every minute
                s.setNextRunAt(calculator.nextRun(s.getCadence(), s.getCron(), s.getTimezone(), now));
                repo.save(s);
            }
        }
    }

    private void dispatchOne(ScheduledReport s, Instant now) {
        ScheduleCalculator.DateWindow window = calculator.windowFor(s.getCadence(), s.getTimezone(), now);
        Map<String, Object> options = parseOptions(s.getParametersJson());

        CreateReportRequest req = new CreateReportRequest(
                s.getReportType(), s.getFormat(), s.getMerchantId(),
                window.from(), window.to(), options, s.getNotifyEmail(), null);
        var job = reportService.submit(req);
        log.info("Scheduled report {} spawned job {} for window {}..{}",
                s.getId(), job.getId(), window.from(), window.to());

        s.setLastRunAt(now);
        s.setNextRunAt(calculator.nextRun(s.getCadence(), s.getCron(), s.getTimezone(), now));
        repo.save(s);
    }

    private Map<String, Object> parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP);
        } catch (Exception e) {
            log.warn("Could not parse schedule options; using empty", e);
            return Map.of();
        }
    }
}
