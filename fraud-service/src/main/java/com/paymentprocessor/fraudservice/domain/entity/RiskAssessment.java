package com.paymentprocessor.fraudservice.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit record of a single fraud evaluation run: the resulting score,
 * decision, and the features/rules that produced it.
 */
@Entity
@Table(
        name = "risk_assessments",
        indexes = {
                @Index(name = "idx_risk_assessments_intent", columnList = "intent_id"),
                @Index(name = "idx_risk_assessments_merchant", columnList = "merchant_id"),
                @Index(name = "idx_risk_assessments_created", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "intent_id", length = 100)
    private String intentId;

    @Column(name = "merchant_id", length = 100)
    private String merchantId;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(length = 30)
    private String decision;

    @Column(name = "triggered_rules", columnDefinition = "TEXT")
    private String triggeredRules;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(length = 100)
    private String model;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "created_at")
    private Instant createdAt;
}
