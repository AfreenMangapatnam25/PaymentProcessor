package com.paymentprocessor.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.analytics.config.AnalyticsProperties;
import com.paymentprocessor.analytics.config.AsyncConfig;
import com.paymentprocessor.analytics.domain.entity.ReportJob;
import com.paymentprocessor.analytics.domain.enums.ReportStatus;
import com.paymentprocessor.analytics.domain.enums.ReportType;
import com.paymentprocessor.analytics.dto.QueryRequest;
import com.paymentprocessor.analytics.repository.ReportJobRepository;
import com.paymentprocessor.analytics.service.export.Exporter;
import com.paymentprocessor.analytics.service.export.ExporterFactory;
import com.paymentprocessor.analytics.service.notification.NotificationDispatcher;
import com.paymentprocessor.analytics.service.query.QueryBuilderService;
import com.paymentprocessor.analytics.service.query.QueryResult;
import com.paymentprocessor.analytics.service.report.ReportData;
import com.paymentprocessor.analytics.service.storage.StorageService;
import com.paymentprocessor.analytics.service.storage.StoredObject;
import com.paymentprocessor.analytics.service.template.ReportParameters;
import com.paymentprocessor.analytics.service.template.TemplateRegistry;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Runs the actual (potentially slow) report generation off the request thread on the
 * bounded report executor. Isolated from ReportService so Spring's async proxy is
 * applied correctly (no self-invocation).
 */
@Component
public class ReportGenerationWorker {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationWorker.class);

    private final ReportJobRepository jobs;
    private final TemplateRegistry templates;
    private final QueryBuilderService queryBuilder;
    private final ExporterFactory exporters;
    private final StorageService storage;
    private final StorageKeyFactory keyFactory;
    private final DownloadUrlResolver urlResolver;
    private final NotificationDispatcher notifications;
    private final ObjectMapper objectMapper;
    private final int retentionDays;

    public ReportGenerationWorker(ReportJobRepository jobs, TemplateRegistry templates,
                                  QueryBuilderService queryBuilder, ExporterFactory exporters,
                                  StorageService storage, StorageKeyFactory keyFactory,
                                  DownloadUrlResolver urlResolver, NotificationDispatcher notifications,
                                  ObjectMapper objectMapper, AnalyticsProperties props) {
        this.jobs = jobs;
        this.templates = templates;
        this.queryBuilder = queryBuilder;
        this.exporters = exporters;
        this.storage = storage;
        this.keyFactory = keyFactory;
        this.urlResolver = urlResolver;
        this.notifications = notifications;
        this.objectMapper = objectMapper;
        this.retentionDays = props.getReports().getRetentionDays();
    }

    @Async(AsyncConfig.REPORT_EXECUTOR)
    public void generateAsync(UUID jobId) {
        ReportJob job = jobs.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Report job {} vanished before generation", jobId);
            return;
        }
        if (job.getStatus() != ReportStatus.QUEUED) {
            log.info("Report job {} not QUEUED (was {}); skipping", jobId, job.getStatus());
            return;
        }
        MDC.put("reportJobId", jobId.toString());
        MDC.put("merchantId", job.getMerchantId());
        long start = System.currentTimeMillis();
        try {
            job.markRunning();
            job = jobs.saveAndFlush(job); // keep the managed, version-incremented instance

            ReportData data = buildData(job);
            String key = keyFactory.keyFor(job);
            Exporter exporter = exporters.get(job.getFormat());
            StoredObject stored = storage.store(key, job.getFormat().contentType(),
                    out -> exporter.write(data, out));

            Instant expiresAt = Instant.now().plus(retentionDays, ChronoUnit.DAYS);
            job.markCompleted(stored.key(), data.totalRows(), stored.sizeBytes(), expiresAt);
            job = jobs.saveAndFlush(job);
            log.info("Report job {} completed: {} rows, {} bytes in {} ms",
                    jobId, data.totalRows(), stored.sizeBytes(), System.currentTimeMillis() - start);

            notifications.dispatch(job, urlResolver.resolve(job));
        } catch (Exception e) {
            log.error("Report job {} failed", jobId, e);
            job.markFailed(e.getMessage());
            jobs.saveAndFlush(job);
            notifications.dispatch(job, null);
        } finally {
            MDC.clear();
        }
    }

    private ReportData buildData(ReportJob job) throws Exception {
        if (job.getReportType() == ReportType.AD_HOC) {
            QueryRequest req = objectMapper.readValue(job.getParametersJson(), QueryRequest.class);
            QueryResult result = queryBuilder.run(req);
            return queryBuilder.toReportData("Ad-hoc Report", result);
        }
        ReportParameters params = objectMapper.readValue(job.getParametersJson(), ReportParameters.class);
        return templates.get(job.getReportType()).generate(params);
    }
}
