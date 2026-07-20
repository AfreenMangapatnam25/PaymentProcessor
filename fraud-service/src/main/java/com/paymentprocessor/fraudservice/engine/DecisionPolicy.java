package com.paymentprocessor.fraudservice.engine;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Maps a normalized 0-100 risk score to a {@link Decision} using the bands
 * documented in FraudReadme.md, then applies routing overrides (hard flags,
 * whitelist caps, mandatory review for high-value transactions).
 *
 * <p>Bands (base):
 * <pre>
 *   0-25   Low            -> APPROVE
 *   26-40  Low-Medium     -> APPROVE (with monitoring)
 *   41-60  Medium         -> CHALLENGE (3DS)
 *   61-75  High           -> REVIEW
 *   76-100 Very/Critical  -> DECLINE
 * </pre>
 */
@Component
public class DecisionPolicy {

    @Value("${fraud.scoring.challengeFloor:41}")
    private int challengeFloor;
    @Value("${fraud.scoring.reviewFloor:61}")
    private int reviewFloor;
    @Value("${fraud.scoring.declineFloor:76}")
    private int declineFloor;

    /** Transactions at/above this amount are always routed to manual review. */
    @Value("${fraud.scoring.mandatoryReviewAmount:10000}")
    private BigDecimal mandatoryReviewAmount;

    /** Score multiplier applied when the entity is whitelisted (dampens risk). */
    @Value("${fraud.scoring.whitelistFactor:0.5}")
    private double whitelistFactor;

    /** Max points the ML layer can contribute to the raw score. */
    @Value("${fraud.scoring.mlMaxPoints:40}")
    private double mlMaxPoints;

    public double getWhitelistFactor() { return whitelistFactor; }

    public double getMlMaxPoints() { return mlMaxPoints; }

    public BigDecimal getMandatoryReviewAmount() { return mandatoryReviewAmount; }

    /**
     * Resolve the final decision from the score and any hard flags/overrides.
     */
    public Decision decide(int score, RiskContext ctx) {
        // Priority ordering: hard outcomes short-circuit the bands.
        if (ctx.isHardEscalate()) {
            return Decision.ESCALATE;
        }
        if (ctx.isHardDecline()) {
            return Decision.DECLINE;
        }

        Decision base = fromScore(score);

        // AML alert (non-sanctions, e.g. structuring/PEP) forces at least REVIEW.
        if (ctx.isAmlAlert()) {
            base = atLeast(base, Decision.REVIEW);
        }

        // High-value transactions are reviewed regardless of score.
        if (ctx.isMandatoryReview()) {
            base = atLeast(base, Decision.REVIEW);
        }

        // Whitelisted entities are never auto-declined by score alone
        // (blacklist/AML hard flags already handled above).
        if (ctx.isWhitelisted() && base == Decision.DECLINE) {
            base = Decision.REVIEW;
        }

        return base;
    }

    private Decision fromScore(int score) {
        if (score >= declineFloor) {
            return Decision.DECLINE;
        }
        if (score >= reviewFloor) {
            return Decision.REVIEW;
        }
        if (score >= challengeFloor) {
            return Decision.CHALLENGE;
        }
        return Decision.APPROVE;
    }

    /** Return whichever of the two decisions is more restrictive. */
    private Decision atLeast(Decision current, Decision floor) {
        return severity(current) >= severity(floor) ? current : floor;
    }

    private int severity(Decision d) {
        switch (d) {
            case APPROVE:   return 0;
            case CHALLENGE: return 1;
            case REVIEW:    return 2;
            case DECLINE:   return 3;
            case ESCALATE:  return 4;
            default:        return 0;
        }
    }

    public String recommendedAction(Decision decision) {
        switch (decision) {
            case APPROVE:   return "PROCEED";
            case CHALLENGE: return "INITIATE_3DS";
            case REVIEW:    return "HOLD_FOR_REVIEW";
            case DECLINE:   return "BLOCK";
            case ESCALATE:  return "ALERT_COMPLIANCE";
            default:        return "PROCEED";
        }
    }
}
