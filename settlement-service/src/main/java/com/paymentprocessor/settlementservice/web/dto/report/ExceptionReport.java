package com.paymentprocessor.settlementservice.web.dto.report;

import com.paymentprocessor.settlementservice.web.dto.BatchResponse;
import com.paymentprocessor.settlementservice.web.dto.PayoutResponse;
import java.time.Instant;
import java.util.List;

/** Operations report of failed, returned, and reversed settlements. */
public record ExceptionReport(
        Instant generatedAt,
        List<BatchResponse> problemBatches,
        List<PayoutResponse> problemPayouts
) {
}
