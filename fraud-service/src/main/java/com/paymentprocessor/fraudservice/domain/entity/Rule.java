package com.paymentprocessor.fraudservice.domain.entity;

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
 * A versioned, data-driven fraud rule whose SpEL expression is evaluated
 * against the accumulated feature map by {@code RuleEngineEvaluator}.
 */
@Entity
@Table(
        name = "rules",
        indexes = {
                @Index(name = "idx_rules_name", columnList = "name"),
                @Index(name = "idx_rules_enabled", columnList = "enabled"),
                @Index(name = "idx_rules_scope", columnList = "scope")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 50)
    private String scope;

    @Column(length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String expr;

    @Column(length = 20)
    private String action;

    @Column
    private Integer priority;

    @Column
    private Boolean enabled;

    @Column
    private Integer version;
}
