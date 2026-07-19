package com.paymentprocessor.settlementservice.web.dto.report;

import com.paymentprocessor.settlementservice.web.dto.BatchResponse;
import com.paymentprocessor.settlementservice.web.dto.PayoutResponse;
import com.paymentprocessor.settlementservice.web.dto.ReserveResponse;
import java.time.Instant;
import java.util.List;

/** Merchant-facing settlement statement: batches, payouts, and reserves. */
public record MerchantStatementReport(
        String merchantId,
        Instant generatedAt,
        long totalNetSettledMinor,
        List<BatchResponse> batches,
        List<PayoutResponse> payouts,
        List<ReserveResponse> reserves
) {
}
