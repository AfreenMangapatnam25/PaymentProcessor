package com.paymentprocessor.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.analytics.config.AnalyticsProperties;
import com.paymentprocessor.analytics.domain.entity.ReportJob;
import com.paymentprocessor.analytics.domain.enums.ReportStatus;
import com.paymentprocessor.analytics.domain.enums.ReportType;
import com.paymentprocessor.analytics.dto.CreateReportRequest;
import com.paymentprocessor.analytics.dto.QueryRequest;
import com.paymentprocessor.analytics.exception.ConcurrencyLimitException;
import com.paymentprocessor.analytics.exception.ReportNotFoundException;
import com.paymentprocessor.analytics.exception.QueryValidationException;
import com.paymentprocessor.analytics.repository.ReportJobRepository;
import com.paymentprocessor.analytics.service.query.QueryBuilderService;
import com.paymentprocessor.analytics.service.template.ReportParameters;
import com.paymentprocessor.analytics.service.template.TemplateRegistry;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Entry point for report generation: validates the request, records a QUEUED job, and
 * hands off to the async worker. Only the Postgres job metadata is written here — all
 * report content comes from the read-only ClickHouse replica.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final List<ReportStatus> ACTIVE = List.of(ReportStatus.QUEUED, ReportStatus.RUNNING);

    private final ReportJobRepository jobs;
    private final ReportGenerationWorker worker;
    private final TemplateRegistry templates;
    private final QueryBuilderService queryBuilder;
    private final ObjectMapper objectMapper;
    private final DownloadUrlResolver urlResolver;
    private final int maxConcurrentPerMerchant;

    public ReportService(ReportJobRepository jobs, ReportGenerationWorker worker,
                         TemplateRegistry templates, QueryBuilderService queryBuilder,
                         ObjectMapper objectMapper, DownloadUrlResolver urlResolver,
                         AnalyticsProperties props) {
        this.jobs = jobs;
        this.worker = worker;
        this.templates = templates;
        this.queryBuilder = queryBuilder;
        this.objectMapper = objectMapper;
        this.urlResolver = urlResolver;
        this.maxConcurrentPerMerchant = props.getReports().getMaxConcurrentPerMerchant();
    }

    @Transactional
    public ReportJob submit(CreateReportRequest req) {
        String parametersJson = validateAndSerialize(req);

        long active = jobs.countByMerchantIdAndStatusIn(req.merchantId(), ACTIVE);
        if (active >= maxConcurrentPerMerchant) {
            throw new ConcurrencyLimitException(
                    "Merchant " + req.merchantId() + " has too many in-flight reports ("
                            + active + "/" + maxConcurrentPerMerchant + "). Try again shortly.");
        }

        ReportJob job = new ReportJob(UUID.randomUUID(), req.merchantId(), req.reportType(),
                req.format(), parametersJson, null, req.notifyEmail());
        job = jobs.save(job);
        log.info("Queued report job {} ({} / {}) for merchant {}",
                job.getId(), req.reportType(), req.format(), req.merchantId());

        // Dispatch only after the row is committed, so the async worker (running on a
        // different thread/connection) never races an uncommitted insert.
        final UUID jobId = job.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    worker.generateAsync(jobId);
                }
            });
        } else {
            worker.generateAsync(jobId);
        }
        return job;
    }

    @Transactional(readOnly = true)
    public ReportJob get(UUID id) {
        return jobs.findById(id)
                .orElseThrow(() -> new ReportNotFoundException("No report job " + id));
    }

    @Transactional(readOnly = true)
    public Page<ReportJob> listForMerchant(String merchantId, Pageable pageable) {
        return jobs.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable);
    }

    public String downloadUrl(ReportJob job) {
        return urlResolver.resolve(job);
    }

    private String validateAndSerialize(CreateReportRequest req) {
        try {
            if (req.reportType() == ReportType.AD_HOC) {
                QueryRequest q = req.query();
                if (q == null) {
                    throw new QueryValidationException("AD_HOC reports require a 'query' block");
                }
                queryBuilder.validate(q); // fail fast before queueing
                return objectMapper.writeValueAsString(q);
            }
            if (!templates.supports(req.reportType())) {
                throw new QueryValidationException("Unsupported report type: " + req.reportType());
            }
            if (req.fromDate() == null || req.toDate() == null) {
                throw new QueryValidationException("fromDate and toDate are required for template reports");
            }
            if (req.toDate().isBefore(req.fromDate())) {
                throw new QueryValidationException("toDate must not be before fromDate");
            }
            Map<String, Object> opts = req.options() == null ? Map.of() : req.options();
            ReportParameters params = new ReportParameters(
                    req.merchantId(), req.fromDate(), req.toDate(), opts);
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new QueryValidationException("Could not serialize report parameters: " + e.getMessage());
        }
    }
}
