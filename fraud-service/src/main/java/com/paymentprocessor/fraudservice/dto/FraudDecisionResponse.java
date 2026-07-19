package com.paymentprocessor.fraudservice.dto;

import java.util.List;

/**
 * Final verdict returned to the Payment Service. Mirrors the decision payload
 * documented in FraudReadme.md and adds a full rule breakdown for audit.
 */
public class FraudDecisionResponse {

    private String intentId;
    private String decision;            // APPROVE, CHALLENGE, REVIEW, DECLINE, ESCALATE
    private int riskScore;              // 0-100
    private double confidence;          // 0..1
    private List<String> rulesFired;
    private double mlContribution;      // 0..1 probability from the ML layer
    private String reasonCode;
    private String recommendedAction;   // PROCEED, INITIATE_3DS, HOLD_FOR_REVIEW, BLOCK, ALERT_COMPLIANCE
    private String reviewQueueId;       // non-null when routed to a review/compliance queue
    private String model;
    private long latencyMs;
    private List<RuleHit> breakdown;

    public String getIntentId() { return intentId; }
    public void setIntentId(String intentId) { this.intentId = intentId; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public List<String> getRulesFired() { return rulesFired; }
    public void setRulesFired(List<String> rulesFired) { this.rulesFired = rulesFired; }
    public double getMlContribution() { return mlContribution; }
    public void setMlContribution(double mlContribution) { this.mlContribution = mlContribution; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public String getReviewQueueId() { return reviewQueueId; }
    public void setReviewQueueId(String reviewQueueId) { this.reviewQueueId = reviewQueueId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public List<RuleHit> getBreakdown() { return breakdown; }
    public void setBreakdown(List<RuleHit> breakdown) { this.breakdown = breakdown; }
}
