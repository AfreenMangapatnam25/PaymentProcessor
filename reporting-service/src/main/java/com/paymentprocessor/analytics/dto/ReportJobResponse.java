package com.paymentprocessor.analytics.dto;

import com.paymentprocessor.analytics.domain.entity.ReportJob;
import com.paymentprocessor.analytics.domain.enums.ExportFormat;
import com.paymentprocessor.analytics.domain.enums.ReportStatus;
import com.paymentprocessor.analytics.domain.enums.ReportType;
import java.time.Instant;
import java.util.UUID;

public record ReportJobResponse(
        UUID id,
        String merchantId,
        ReportType reportType,
        ExportFormat format,
        ReportStatus status,
        Long rowCount,
        Long sizeBytes,
        String errorMessage,
        String downloadUrl,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        Instant expiresAt) {

    public static ReportJobResponse from(ReportJob j, String downloadUrl) {
        return new ReportJobResponse(j.getId(), j.getMerchantId(), j.getReportType(), j.getFormat(),
                j.getStatus(), j.getRowCount(), j.getSizeBytes(), j.getErrorMessage(), downloadUrl,
                j.getCreatedAt(), j.getStartedAt(), j.getCompletedAt(), j.getExpiresAt());
    }

    public static ReportJobResponse from(ReportJob j) { return from(j, null); }
}
