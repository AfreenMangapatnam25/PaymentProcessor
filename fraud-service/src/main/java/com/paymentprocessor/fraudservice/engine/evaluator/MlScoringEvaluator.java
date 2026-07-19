package com.paymentprocessor.fraudservice.engine.evaluator;

import org.springframework.stereotype.Component;

import com.paymentprocessor.fraudservice.dto.FraudEvaluationRequest;
import com.paymentprocessor.fraudservice.engine.RiskContext;
import com.paymentprocessor.fraudservice.engine.SignalEvaluator;
import com.paymentprocessor.fraudservice.engine.ml.MlScorer;

/**
 * Final layer: runs the ML model over the accumulated feature map and records
 * the fraud probability on the context. The engine converts this probability
 * into score points (up to {@code mlMaxPoints}).
 */
@Component
public class MlScoringEvaluator implements SignalEvaluator {

    private final MlScorer scorer;

    public MlScoringEvaluator(MlScorer scorer) {
        this.scorer = scorer;
    }

    @Override
    public int order() { return 70; }

    @Override
    public String name() { return "ml"; }

    @Override
    public void evaluate(FraudEvaluationRequest req, RiskContext ctx) {
        int rulePoints = ctx.getHits().stream().mapToInt(h -> h.getPoints()).sum();
        double probability = scorer.score(ctx.getFeatures(), rulePoints);
        ctx.setMlProbability(probability);
        if (probability >= 0.5) {
            ctx.reasonCodeIfAbsent("ML_ANOMALY");
        }
    }
}
