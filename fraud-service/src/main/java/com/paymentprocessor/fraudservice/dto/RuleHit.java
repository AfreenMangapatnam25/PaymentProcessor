package com.paymentprocessor.fraudservice.dto;

/**
 * A single signal that fired during evaluation, contributing points to the
 * composite risk score (or forcing a hard decision). Returned in the decision
 * breakdown for explainability and audit.
 */
public class RuleHit {

    private String name;
    private String category;   // VELOCITY, DEVICE, IP, COUNTRY, BIN, CARD, MERCHANT, USER, AML, LIST, RULE, ML
    private int points;        // contribution to the 0-100 score
    private String action;     // SCORE, CHALLENGE, REVIEW, DECLINE, ESCALATE
    private String reason;

    public RuleHit() {
    }

    public RuleHit(String name, String category, int points, String action, String reason) {
        this.name = name;
        this.category = category;
        this.points = points;
        this.action = action;
        this.reason = reason;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
