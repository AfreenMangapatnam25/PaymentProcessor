package com.paymentprocessor.fraudservice.document;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "risk_assessments")
public class RiskAssessment {

    @Id
    private String id;

    private String intentId;
    private String merchantId;
    private BigDecimal score;
    private String decision;
    private String triggeredRules;
    private String features;
    private String model;
    private Integer latencyMs;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIntentId() { return intentId; }
    public void setIntentId(String intentId) { this.intentId = intentId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getTriggeredRules() { return triggeredRules; }
    public void setTriggeredRules(String triggeredRules) { this.triggeredRules = triggeredRules; }
    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
