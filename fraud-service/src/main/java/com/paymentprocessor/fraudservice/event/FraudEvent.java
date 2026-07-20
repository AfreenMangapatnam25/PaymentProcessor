package com.paymentprocessor.fraudservice.event;

import com.paymentprocessor.fraudservice.dto.FraudDecisionResponse;

/**
 * Domain events published after every evaluation. Consumers (Notification,
 * Compliance/Audit, ML Platform) subscribe via Spring's event bus; in a
 * distributed deployment a listener would relay these to Kafka/SNS.
 */
public abstract class FraudEvent {

    public enum Type {
        /** A rule fired, threshold exceeded, or known fraud pattern matched. */
        FRAUD_DETECTED,
        /** Routed to the human analyst queue. */
        MANUAL_REVIEW_REQUIRED,
        /** Cleared after evaluation (or secondary check). */
        FRAUD_APPROVED,
        /** Definitively declined due to fraud detection. */
        FRAUD_REJECTED,
        /** Sanctions hit / confirmed fraud ring — compliance + security alert. */
        FRAUD_ESCALATED
    }

    private final Type type;
    private final FraudDecisionResponse decision;

    protected FraudEvent(Type type, FraudDecisionResponse decision) {
        this.type = type;
        this.decision = decision;
    }

    public Type getType() { return type; }

    public FraudDecisionResponse getDecision() { return decision; }

    public static FraudEvent of(Type type, FraudDecisionResponse decision) {
        return new FraudEvent(type, decision) {
        };
    }
}
