package com.paymentprocessor.settlementservice.web.dto.report;

import com.paymentprocessor.settlementservice.web.dto.ReserveResponse;
import java.time.Instant;
import java.util.List;

/** Upcoming reserve releases by merchant and date. */
public record ReserveReleaseScheduleReport(
        Instant generatedAt,
        long totalHeldMinor,
        List<ReserveResponse> upcomingReleases
) {
}
