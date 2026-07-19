package com.paymentprocessor.reconciliationservice.service.matching;

import com.paymentprocessor.reconciliationservice.config.ReconProperties;
import com.paymentprocessor.reconciliationservice.domain.ReconRecord;

/**
 * A single, self-contained matching rule. Rules are evaluated in ascending {@link #priority()} order;
 * the first rule to return a non-null {@link MatchEvaluation} for a candidate pair wins.
 */
public interface MatchRule {

    /** Stable rule identifier stored on the resulting match. */
    String name();

    /** Evaluation order; lower runs first (highest-confidence rules should have the lowest priority). */
    int priority();

    /**
     * Evaluate whether an internal record matches an external record.
     *
     * @return a populated evaluation when the pair matches, or {@code null} otherwise.
     */
    MatchEvaluation evaluate(ReconRecord internal, ReconRecord external, ReconProperties.Matching props);
}
