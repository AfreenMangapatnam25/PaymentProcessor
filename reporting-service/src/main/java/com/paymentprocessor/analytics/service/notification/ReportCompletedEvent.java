package com.paymentprocessor.analytics.service.notification;

import com.paymentprocessor.analytics.domain.enums.ExportFormat;
import com.paymentprocessor.analytics.domain.enums.ReportStatus;
import com.paymentprocessor.analytics.domain.enums.ReportType;
import java.time.Instant;
import java.util.UUID;

/** Event published to Kafka when a report reaches a terminal state. */
public record ReportCompletedEvent(
        UUID jobId,
        String merchantId,
        ReportType reportType,
        ExportFormat format,
        ReportStatus status,
        Long rowCount,
        Long sizeBytes,
        String storageKey,
        String downloadUrl,
        String errorMessage,
        Instant occurredAt) {
}
