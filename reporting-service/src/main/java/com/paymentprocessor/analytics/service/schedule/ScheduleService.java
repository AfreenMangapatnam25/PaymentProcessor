package com.paymentprocessor.analytics.service.schedule;

import com.paymentprocessor.analytics.domain.entity.ScheduledReport;
import com.paymentprocessor.analytics.domain.enums.ReportType;
import com.paymentprocessor.analytics.dto.CreateScheduleRequest;
import com.paymentprocessor.analytics.dto.UpdateScheduleRequest;
import com.paymentprocessor.analytics.exception.QueryValidationException;
import com.paymentprocessor.analytics.exception.ReportNotFoundException;
import com.paymentprocessor.analytics.repository.ScheduledReportRepository;
import com.paymentprocessor.analytics.service.template.TemplateRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD + lifecycle for recurring report definitions. */
@Service
public class ScheduleService {

    private final ScheduledReportRepository repo;
    private final ScheduleCalculator calculator;
    private final TemplateRegistry templates;
    private final ObjectMapper objectMapper;

    public ScheduleService(ScheduledReportRepository repo, ScheduleCalculator calculator,
                           TemplateRegistry templates, ObjectMapper objectMapper) {
        this.repo = repo;
        this.calculator = calculator;
        this.templates = templates;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ScheduledReport create(CreateScheduleRequest req) {
        if (req.reportType() == ReportType.AD_HOC || !templates.supports(req.reportType())) {
            throw new QueryValidationException("Scheduled reports must use a template type, got " + req.reportType());
        }
        String tz = req.timezone() == null ? "UTC" : req.timezone();
        ScheduledReport s = new ScheduledReport(UUID.randomUUID(), req.merchantId(), req.reportType(),
                req.format(), req.cadence(), req.cron(), tz, toJson(req.options()), req.notifyEmail());
        if (req.enabled() != null) {
            s.setEnabled(req.enabled());
        }
        s.setNextRunAt(calculator.nextRun(s.getCadence(), s.getCron(), s.getTimezone(), Instant.now()));
        return repo.save(s);
    }

    @Transactional
    public ScheduledReport update(UUID id, UpdateScheduleRequest req) {
        ScheduledReport s = require(id);
        if (req.format() != null) s.setFormat(req.format());
        if (req.cadence() != null) s.setCadence(req.cadence());
        if (req.cron() != null) s.setCron(req.cron());
        if (req.options() != null) s.setParametersJson(toJson(req.options()));
        if (req.notifyEmail() != null) s.setNotifyEmail(req.notifyEmail());
        if (req.enabled() != null) s.setEnabled(req.enabled());
        s.setNextRunAt(calculator.nextRun(s.getCadence(), s.getCron(), s.getTimezone(), Instant.now()));
        return repo.save(s);
    }

    @Transactional
    public void delete(UUID id) {
        repo.delete(require(id));
    }

    @Transactional(readOnly = true)
    public ScheduledReport get(UUID id) { return require(id); }

    @Transactional(readOnly = true)
    public Page<ScheduledReport> listForMerchant(String merchantId, Pageable pageable) {
        return repo.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable);
    }

    private ScheduledReport require(UUID id) {
        return repo.findById(id).orElseThrow(() -> new ReportNotFoundException("No scheduled report " + id));
    }

    private String toJson(Map<String, Object> options) {
        try {
            return objectMapper.writeValueAsString(options == null ? Map.of() : options);
        } catch (JsonProcessingException e) {
            throw new QueryValidationException("Invalid options: " + e.getMessage());
        }
    }
}
