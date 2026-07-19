package com.paymentprocessor.reconciliationservice.dto;

import com.paymentprocessor.reconciliationservice.domain.MatchRecord;

import java.math.BigDecimal;
import java.time.Instant;

public record MatchResponse(
        Long id,
        Long reconRunId,
        Long internalRecordId,
        Long externalRecordId,
        String matchType,
        String matchRule,
        BigDecimal confidence,
        BigDecimal amountVariance,
        Integer dateVarianceDays,
        String note,
        boolean manual,
        Instant matchedAt
) {
    public static MatchResponse from(MatchRecord m) {
        return new MatchResponse(
                m.getId(), m.getReconRun().getId(), m.getInternalRecord().getId(), m.getExternalRecord().getId(),
                m.getMatchType().name(), m.getMatchRule(), m.getConfidence(), m.getAmountVariance(),
                m.getDateVarianceDays(), m.getNote(), m.isManual(), m.getMatchedAt());
    }
}
