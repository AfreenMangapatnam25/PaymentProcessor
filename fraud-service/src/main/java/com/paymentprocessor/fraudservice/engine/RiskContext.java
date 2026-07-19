package com.paymentprocessor.fraudservice.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.paymentprocessor.fraudservice.dto.RuleHit;

/**
 * Mutable accumulator threaded through the evaluator pipeline. Each evaluator
 * reads the {@link #features} map and records {@link RuleHit}s and/or raises
 * hard flags. The engine then aggregates everything into a final score/decision.
 */
public class RiskContext {

    private final Map<String, Object> features = new LinkedHashMap<>();
    private final List<RuleHit> hits = new ArrayList<>();

    private boolean whitelisted;
    private boolean hardDecline;     // blacklist hit
    private boolean hardEscalate;    // sanctions / confirmed fraud ring
    private boolean mandatoryReview; // e.g. high-value or AML flag
    private boolean amlAlert;
    private double mlProbability;    // 0..1 from the ML layer
    private String reasonCode;

    public Map<String, Object> getFeatures() { return features; }

    public Object feature(String key) { return features.get(key); }

    public void putFeature(String key, Object value) { features.put(key, value); }

    public List<RuleHit> getHits() { return hits; }

    /** Record a scoring hit (points contribute additively to the risk score). */
    public void score(String name, String category, int points, String reason) {
        hits.add(new RuleHit(name, category, points, RuleAction.SCORE.name(), reason));
    }

    /** Record a hit that carries an explicit action. */
    public void add(RuleHit hit) {
        hits.add(hit);
    }

    public boolean isWhitelisted() { return whitelisted; }
    public void setWhitelisted(boolean whitelisted) { this.whitelisted = whitelisted; }

    public boolean isHardDecline() { return hardDecline; }
    public void setHardDecline(boolean hardDecline) { this.hardDecline = hardDecline; }

    public boolean isHardEscalate() { return hardEscalate; }
    public void setHardEscalate(boolean hardEscalate) { this.hardEscalate = hardEscalate; }

    public boolean isMandatoryReview() { return mandatoryReview; }
    public void setMandatoryReview(boolean mandatoryReview) { this.mandatoryReview = mandatoryReview; }

    public boolean isAmlAlert() { return amlAlert; }
    public void setAmlAlert(boolean amlAlert) { this.amlAlert = amlAlert; }

    public double getMlProbability() { return mlProbability; }
    public void setMlProbability(double mlProbability) { this.mlProbability = mlProbability; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    /** Set the reason code only if one has not already been assigned (first/highest priority wins). */
    public void reasonCodeIfAbsent(String code) {
        if (this.reasonCode == null) {
            this.reasonCode = code;
        }
    }
}
