package com.paymentprocessor.fraudservice.engine;

/**
 * The effect an individual signal/rule hit has on the pipeline.
 *
 * <p>{@link #SCORE} only contributes points; the others are "hard" outcomes
 * that short-circuit the score bands (subject to priority ordering:
 * blacklist &rarr; AML &rarr; static rules &rarr; ML).
 */
public enum RuleAction {
    SCORE,
    CHALLENGE,
    REVIEW,
    DECLINE,
    ESCALATE
}
