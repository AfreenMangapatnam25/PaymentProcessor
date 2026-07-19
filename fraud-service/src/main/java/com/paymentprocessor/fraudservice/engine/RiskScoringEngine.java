package com.paymentprocessor.fraudservice.engine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.fraudservice.domain.entity.RiskAssessment;
import com.paymentprocessor.fraudservice.dto.FraudDecisionResponse;
import com.paymentprocessor.fraudservice.dto.FraudEvaluationRequest;
import com.paymentprocessor.fraudservice.dto.RuleHit;
import com.paymentprocessor.fraudservice.engine.ml.MlScorer;
import com.paymentprocessor.fraudservice.event.FraudEventPublisher;
import com.paymentprocessor.fraudservice.repository.RiskAssessmentRepository;

/**
 * Orchestrates the score-then-decide pipeline:
 * <ol>
 *   <li>Seed the feature map from the request.</li>
 *   <li>Run every {@link SignalEvaluator} in priority order.</li>
 *   <li>Aggregate rule points + ML probability into a 0-100 score.</li>
 *   <li>Resolve the final {@link Decision} via {@link DecisionPolicy}.</li>
 *   <li>Persist the {@link RiskAssessment} and publish a domain event.</li>
 * </ol>
 */
@Service
public class RiskScoringEngine {

    private static final Logger log = LoggerFactory.getLogger(RiskScoringEngine.class);

    private final List<SignalEvaluator> evaluators;
    private final DecisionPolicy policy;
    private final RiskAssessmentRepository assessments;
    private final FraudEventPublisher events;
    private final ObjectMapper objectMapper;

    public RiskScoringEngine(List<SignalEvaluator> evaluators,
                             DecisionPolicy policy,
                             RiskAssessmentRepository assessments,
                             FraudEventPublisher events,
                             ObjectMapper objectMapper) {
        // Enforce deterministic priority ordering regardless of bean discovery order.
        this.evaluators = new ArrayList<>(evaluators);
        this.evaluators.sort(Comparator.comparingInt(SignalEvaluator::order));
        this.policy = policy;
        this.assessments = assessments;
        this.events = events;
        this.objectMapper = objectMapper;
    }

    public FraudDecisionResponse evaluate(FraudEvaluationRequest req) {
        long startNanos = System.nanoTime();
        RiskContext ctx = new RiskContext();
        seedFeatures(req, ctx);

        for (SignalEvaluator evaluator : evaluators) {
            try {
                evaluator.evaluate(req, ctx);
            } catch (Exception e) {
                // A failing layer must never take down the whole decision.
                log.error("Evaluator '{}' failed for intent={}: {}",
                        evaluator.name(), req.getIntentId(), e.getMessage(), e);
            }
        }

        int score = aggregateScore(ctx);
        Decision decision = policy.decide(score, ctx);
        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;

        FraudDecisionResponse response = buildResponse(req, ctx, score, decision, latencyMs);
        persist(req, ctx, response);
        events.publish(response);
        return response;
    }

    // --- Feature seeding ------------------------------------------------------

    private void seedFeatures(FraudEvaluationRequest req, RiskContext ctx) {
        ctx.putFeature("amount", req.getAmount() == null ? 0.0 : req.getAmount().doubleValue());
        ctx.putFeature("currency", req.getCurrency());
        ctx.putFeature("cardType", req.getCardType());
        ctx.putFeature("binCountry", req.getBinCountry());
        ctx.putFeature("billingCountry", req.getBillingCountry());
        ctx.putFeature("ipCountry", req.getIpCountry());
        ctx.putFeature("ipType", req.getIpType());
        ctx.putFeature("deviceNew", Boolean.TRUE.equals(req.getDeviceNew()));
        ctx.putFeature("emulator", Boolean.TRUE.equals(req.getEmulator()));
        ctx.putFeature("rooted", Boolean.TRUE.equals(req.getRooted()));
        ctx.putFeature("vpn", Boolean.TRUE.equals(req.getVpn()));
        ctx.putFeature("disposableEmail", Boolean.TRUE.equals(req.getDisposableEmail()));
        ctx.putFeature("newCustomer", Boolean.TRUE.equals(req.getUserNewCustomer()));
        ctx.putFeature("userAccountAgeDays", req.getUserAccountAgeDays());
        ctx.putFeature("userKycStatus", req.getUserKycStatus());
        ctx.putFeature("userDisputeCount", req.getUserDisputeCount());
        ctx.putFeature("merchantChargebackRate", req.getMerchantChargebackRate());
        ctx.putFeature("merchantMcc", req.getMerchantMcc());
        ctx.putFeature("merchantHighRisk", Boolean.TRUE.equals(req.getMerchantHighRisk()));
        ctx.putFeature("merchantAccountAgeDays", req.getMerchantAccountAgeDays());
        // Velocity/geo features are added by their respective evaluators.
        ctx.putFeature("cardVelocity1h", 0);
        ctx.putFeature("ipVelocity15m", 0);
        ctx.putFeature("geoMismatch", false);

        // Mandatory-review floor for high-value transactions.
        if (req.getAmount() != null
                && req.getAmount().compareTo(policy.getMandatoryReviewAmount()) >= 0) {
            ctx.setMandatoryReview(true);
        }
    }

    // --- Aggregation ----------------------------------------------------------

    private int aggregateScore(RiskContext ctx) {
        int rulePoints = ctx.getHits().stream().mapToInt(RuleHit::getPoints).sum();
        double mlPoints = ctx.getMlProbability() * policy.getMlMaxPoints();
        double raw = rulePoints + mlPoints;
        if (ctx.isWhitelisted()) {
            raw *= policy.getWhitelistFactor();
        }
        return (int) Math.round(Math.max(0, Math.min(100, raw)));
    }

    // --- Response assembly ----------------------------------------------------

    private FraudDecisionResponse buildResponse(FraudEvaluationRequest req, RiskContext ctx,
                                                int score, Decision decision, long latencyMs) {
        FraudDecisionResponse r = new FraudDecisionResponse();
        r.setIntentId(req.getIntentId());
        r.setDecision(decision.name());
        r.setRiskScore(score);
        r.setMlContribution(round2(ctx.getMlProbability()));
        r.setConfidence(confidence(ctx, decision));
        r.setModel(MlScorer.MODEL_NAME);
        r.setLatencyMs(latencyMs);
        r.setRecommendedAction(policy.recommendedAction(decision));
        r.setReasonCode(resolveReasonCode(ctx, decision));

        List<RuleHit> fired = ctx.getHits().stream()
                .filter(h -> h.getPoints() > 0 || !RuleAction.SCORE.name().equals(h.getAction()))
                .collect(Collectors.toList());
        r.setBreakdown(fired);
        r.setRulesFired(fired.stream().map(RuleHit::getName).collect(Collectors.toList()));

        if (decision == Decision.REVIEW || decision == Decision.ESCALATE) {
            r.setReviewQueueId(UUID.randomUUID().toString());
        }
        return r;
    }

    private String resolveReasonCode(RiskContext ctx, Decision decision) {
        if (ctx.getReasonCode() != null) {
            return ctx.getReasonCode();
        }
        return decision == Decision.APPROVE ? "LOW_RISK" : "RISK_ASSESSED";
    }

    private double confidence(RiskContext ctx, Decision decision) {
        if (ctx.isHardDecline() || ctx.isHardEscalate()) {
            return 0.99;
        }
        double mlCertainty = Math.abs(ctx.getMlProbability() - 0.5) * 2; // 0..1
        long corroborating = ctx.getHits().stream().filter(h -> h.getPoints() > 0).count();
        double conf = 0.55 + 0.30 * mlCertainty + 0.03 * corroborating;
        return round2(Math.max(0.0, Math.min(0.99, conf)));
    }

    // --- Persistence ----------------------------------------------------------

    private void persist(FraudEvaluationRequest req, RiskContext ctx, FraudDecisionResponse r) {
        try {
            RiskAssessment a = new RiskAssessment();
            a.setIntentId(req.getIntentId());
            a.setMerchantId(req.getMerchantId());
            a.setScore(BigDecimal.valueOf(r.getRiskScore()));
            a.setDecision(r.getDecision());
            a.setTriggeredRules(toJson(r.getRulesFired()));
            a.setFeatures(toJson(ctx.getFeatures()));
            a.setModel(r.getModel());
            a.setLatencyMs((int) Math.min(Integer.MAX_VALUE, r.getLatencyMs()));
            a.setCreatedAt(Instant.now());
            assessments.save(a);
        } catch (Exception e) {
            // Persistence is best-effort for audit; never block the decision on it.
            log.error("Failed to persist risk assessment for intent={}: {}",
                    req.getIntentId(), e.getMessage());
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
