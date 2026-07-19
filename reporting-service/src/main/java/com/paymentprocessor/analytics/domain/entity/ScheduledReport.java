package com.paymentprocessor.analytics.domain.entity;

import com.paymentprocessor.analytics.domain.enums.ExportFormat;
import com.paymentprocessor.analytics.domain.enums.ReportType;
import com.paymentprocessor.analytics.domain.enums.ScheduleCadence;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A recurring report definition. A dispatcher materializes these into ReportJobs
 * on the requested cadence.
 */
@Entity
@Table(name = "scheduled_report", indexes = {
        @Index(name = "idx_sched_merchant", columnList = "merchant_id"),
        @Index(name = "idx_sched_enabled_next", columnList = "enabled,next_run_at")
})
public class ScheduledReport {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 40)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 10)
    private ExportFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "cadence", nullable = false, length = 20)
    private ScheduleCadence cadence;

    /** Cron override; when null the cadence default is used. */
    @Column(name = "cron")
    private String cron;

    @Column(name = "timezone", nullable = false, length = 60)
    private String timezone = "UTC";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private String parametersJson;

    @Column(name = "notify_email")
    private String notifyEmail;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ScheduledReport() { }

    public ScheduledReport(UUID id, String merchantId, ReportType reportType, ExportFormat format,
                           ScheduleCadence cadence, String cron, String timezone,
                           String parametersJson, String notifyEmail) {
        this.id = id;
        this.merchantId = merchantId;
        this.reportType = reportType;
        this.format = format;
        this.cadence = cadence;
        this.cron = cron;
        this.timezone = timezone != null ? timezone : "UTC";
        this.parametersJson = parametersJson;
        this.notifyEmail = notifyEmail;
        this.enabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void touch() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getMerchantId() { return merchantId; }
    public ReportType getReportType() { return reportType; }
    public ExportFormat getFormat() { return format; }
    public ScheduleCadence getCadence() { return cadence; }
    public String getCron() { return cron; }
    public String getTimezone() { return timezone; }
    public String getParametersJson() { return parametersJson; }
    public String getNotifyEmail() { return notifyEmail; }
    public boolean isEnabled() { return enabled; }
    public Instant getNextRunAt() { return nextRunAt; }
    public Instant getLastRunAt() { return lastRunAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; touch(); }
    public void setFormat(ExportFormat format) { this.format = format; touch(); }
    public void setCadence(ScheduleCadence cadence) { this.cadence = cadence; touch(); }
    public void setCron(String cron) { this.cron = cron; touch(); }
    public void setParametersJson(String json) { this.parametersJson = json; touch(); }
    public void setNotifyEmail(String email) { this.notifyEmail = email; touch(); }
    public void setNextRunAt(Instant t) { this.nextRunAt = t; }
    public void setLastRunAt(Instant t) { this.lastRunAt = t; }
}
