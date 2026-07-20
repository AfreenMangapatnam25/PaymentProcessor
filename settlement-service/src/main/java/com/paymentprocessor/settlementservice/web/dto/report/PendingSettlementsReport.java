package com.paymentprocessor.settlementservice.web.dto.report;

import com.paymentprocessor.settlementservice.web.dto.BatchResponse;
import com.paymentprocessor.settlementservice.web.dto.PayoutResponse;
import java.time.Instant;
import java.util.List;

/** Operations report of settlements awaiting execution or in retry. */
public record PendingSettlementsReport(
        Instant generatedAt,
        List<BatchResponse> pendingBatches,
        List<PayoutResponse> pendingPayouts
) {
}
