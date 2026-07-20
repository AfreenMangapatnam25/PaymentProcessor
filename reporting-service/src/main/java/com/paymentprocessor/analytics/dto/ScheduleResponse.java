package com.paymentprocessor.analytics.dto;

import com.paymentprocessor.analytics.domain.entity.ScheduledReport;
import com.paymentprocessor.analytics.domain.enums.ExportFormat;
import com.paymentprocessor.analytics.domain.enums.ReportType;
import com.paymentprocessor.analytics.domain.enums.ScheduleCadence;
import java.time.Instant;
import java.util.UUID;

public record ScheduleResponse(
        UUID id,
        String merchantId,
        ReportType reportType,
        ExportFormat format,
        ScheduleCadence cadence,
        String cron,
        String timezone,
        boolean enabled,
        Instant nextRunAt,
        Instant lastRunAt,
        Instant createdAt) {

    public static ScheduleResponse from(ScheduledReport s) {
        return new ScheduleResponse(s.getId(), s.getMerchantId(), s.getReportType(), s.getFormat(),
                s.getCadence(), s.getCron(), s.getTimezone(), s.isEnabled(),
                s.getNextRunAt(), s.getLastRunAt(), s.getCreatedAt());
    }
}
