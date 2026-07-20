package com.paymentprocessor.settlementservice.web.dto.report;

import com.paymentprocessor.settlementservice.enums.BatchStatus;
import java.time.Instant;
import java.util.List;

/** Finance daily summary: batch counts and net totals by currency and status. */
public record DailySummaryReport(
        Instant generatedAt,
        List<Line> lines
) {
    public record Line(String currency, BatchStatus status, long batchCount, long totalNetMinor) {
    }
}
