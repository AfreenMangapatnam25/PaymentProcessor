package com.paymentprocessor.analytics.dto;

import com.paymentprocessor.analytics.domain.enums.ExportFormat;
import com.paymentprocessor.analytics.domain.enums.ScheduleCadence;
import jakarta.validation.constraints.Email;
import java.util.Map;

/** Partial update; null fields are left unchanged. */
public record UpdateScheduleRequest(
        ExportFormat format,
        ScheduleCadence cadence,
        String cron,
        Map<String, Object> options,
        @Email String notifyEmail,
        Boolean enabled) {
}
