package com.paymentprocessor.analytics.domain.entity;

import com.paymentprocessor.analytics.domain.enums.ExportFormat;
import com.paymentprocessor.analytics.domain.enums.ReportStatus;
import com.paymentprocessor.analytics.domain.enums.ReportType;
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
 * Operational record of an async report generation request.
 * Lives in Postgres — this is the one thing the analytics service actually owns.
 */
@Entity
@Table(name = "report_job", indexes = {
        @Index(name = "idx_report_job_merchant", columnList = "merchant_id"),
        @Index(name = "idx_report_job_status", columnList = "status"),
        @Index(name = "idx_report_job_created", columnList = "created_at")
})
public class ReportJob {

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
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    /** Report parameters (date range, filters, ad-hoc spec) serialized as JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private String parametersJson;

    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "notify_email")
    private String notifyEmail;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "row_count")
    private Long rowCount;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "scheduled_report_id")
    private UUID scheduledReportId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ReportJob() { }

    public ReportJob(UUID id, String merchantId, ReportType reportType, ExportFormat format,
                     String parametersJson, String requestedBy, String notifyEmail) {
        this.id = id;
        this.merchantId = merchantId;
        this.reportType = reportType;
        this.format = format;
        this.parametersJson = parametersJson;
        this.requestedBy = requestedBy;
        this.notifyEmail = notifyEmail;
        this.status = ReportStatus.QUEUED;
        this.createdAt = Instant.now();
    }

    public void markRunning() {
        this.status = ReportStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void markCompleted(String storageKey, long rowCount, long sizeBytes, Instant expiresAt) {
        this.status = ReportStatus.COMPLETED;
        this.storageKey = storageKey;
        this.rowCount = rowCount;
        this.sizeBytes = sizeBytes;
        this.completedAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public void markFailed(String message) {
        this.status = ReportStatus.FAILED;
        this.errorMessage = message != null && message.length() > 2000 ? message.substring(0, 2000) : message;
        this.completedAt = Instant.now();
    }

    public void markExpired() { this.status = ReportStatus.EXPIRED; }

    public UUID getId() { return id; }
    public String getMerchantId() { return merchantId; }
    public ReportType getReportType() { return reportType; }
    public ExportFormat getFormat() { return format; }
    public ReportStatus getStatus() { return status; }
    public String getParametersJson() { return parametersJson; }
    public String getRequestedBy() { return requestedBy; }
    public String getNotifyEmail() { return notifyEmail; }
    public String getStorageKey() { return storageKey; }
    public Long getRowCount() { return rowCount; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getErrorMessage() { return errorMessage; }
    public UUID getScheduledReportId() { return scheduledReportId; }
    public void setScheduledReportId(UUID id) { this.scheduledReportId = id; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
