package com.paymentprocessor.reconciliationservice.service.matching;

import com.paymentprocessor.reconciliationservice.domain.MatchType;

import java.math.BigDecimal;

/**
 * The outcome of evaluating a single (internal, external) candidate pair against a {@link MatchRule}.
 * A {@code null} evaluation means the rule did not match the pair.
 */
public record MatchEvaluation(
        String ruleName,
        MatchType matchType,
        BigDecimal confidence,
        BigDecimal amountVariance,
        Integer dateVarianceDays,
        String note
) {
}
