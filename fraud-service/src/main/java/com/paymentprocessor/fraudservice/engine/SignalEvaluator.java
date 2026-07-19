package com.paymentprocessor.fraudservice.engine;

import com.paymentprocessor.fraudservice.dto.FraudEvaluationRequest;

/**
 * A single layer of the defense pipeline. Implementations are Spring beans and
 * are executed in {@link #order()} sequence by the {@link RiskScoringEngine}.
 *
 * <p>Priority ordering follows FraudReadme.md: blacklist &rarr; AML &rarr;
 * whitelist &rarr; static/velocity/entity rules &rarr; ML.
 */
public interface SignalEvaluator {

    /** Lower runs first. */
    int order();

    /** Short human-readable name for logging. */
    String name();

    /** Inspect the request/features and mutate the context. */
    void evaluate(FraudEvaluationRequest request, RiskContext context);
}
