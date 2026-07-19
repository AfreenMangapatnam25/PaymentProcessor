package com.paymentprocessor.analytics.dto;

import com.paymentprocessor.analytics.domain.enums.ExportFormat;
import com.paymentprocessor.analytics.domain.enums.ReportType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;

/**
 * Request to generate a report asynchronously. For template reports supply
 * {@code fromDate}/{@code toDate}; for {@code AD_HOC} supply {@code query}.
 */
public record CreateReportRequest(
        @NotNull ReportType reportType,
        @NotNull ExportFormat format,
        @NotBlank String merchantId,
        LocalDate fromDate,
        LocalDate toDate,
        Map<String, Object> options,
        @Email String notifyEmail,
        @Valid QueryRequest query) {
}
