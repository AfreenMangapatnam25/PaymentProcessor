package com.paymentprocessor.analytics.dto;

import com.paymentprocessor.analytics.domain.enums.ExportFormat;
import com.paymentprocessor.analytics.domain.enums.ReportType;
import com.paymentprocessor.analytics.domain.enums.ScheduleCadence;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateScheduleRequest(
        @NotNull ReportType reportType,
        @NotNull ExportFormat format,
        @NotBlank String merchantId,
        @NotNull ScheduleCadence cadence,
        String cron,
        String timezone,
        Map<String, Object> options,
        @Email String notifyEmail,
        Boolean enabled) {
}
