package com.paymentprocessor.fraudservice.engine.ml;

import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Stand-in for the supervised ML layer. Produces a deterministic, explainable
 * fraud probability (0..1) via a logistic function over engineered features.
 *
 * <p>In production this delegates to a served model (XGBoost / neural net) or an
 * external provider (Sift, ThreatMetrix). The interface — a probability plus a
 * model identifier — stays the same so the rest of the pipeline is unaffected.
 */
@Component
public class MlScorer {

    public static final String MODEL_NAME = "heuristic-logistic-v1";

    /**
     * @param features the accumulated feature map from {@code RiskContext}
     * @param rulePoints total points already accrued from rule/signal hits
     * @return fraud probability in [0,1]
     */
    public double score(Map<String, Object> features, int rulePoints) {
        // Logistic regression with hand-set weights over normalized features.
        double z = -2.2; // bias -> low baseline probability

        z += 0.9 * asBinary(features.get("deviceNew"));
        z += 1.6 * asBinary(features.get("emulator"));
        z += 1.1 * asBinary(features.get("vpn"));
        z += 1.4 * asBinary(features.get("disposableEmail"));
        z += 1.0 * asBinary(features.get("geoMismatch"));
        z += 0.8 * asBinary(features.get("newCustomer"));

        // Amount, log-scaled and normalized around a typical ticket size.
        double amount = asDouble(features.get("amount"));
        if (amount > 0) {
            z += 0.35 * Math.log10(1 + amount / 100.0);
        }

        // Velocity pressure: more concurrent hits -> higher probability.
        z += 0.20 * asDouble(features.get("cardVelocity1h"));
        z += 0.12 * asDouble(features.get("ipVelocity15m"));

        // Let deterministic rule pressure nudge the model.
        z += 0.015 * rulePoints;

        return sigmoid(z);
    }

    private double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }

    private double asBinary(Object v) {
        return Boolean.TRUE.equals(v) ? 1.0 : 0.0;
    }

    private double asDouble(Object v) {
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        return 0.0;
    }
}
