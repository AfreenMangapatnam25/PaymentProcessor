package com.paymentprocessor.authorization.domain.access;

import com.paymentprocessor.authorization.domain.enums.PolicyEffect;
import com.paymentprocessor.authorization.domain.enums.PolicyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * An ABAC / conditional access policy. The {@code condition} column holds a JSON array of clauses
 * (attribute / operator / value) that are AND-combined and evaluated against the decision context.
 */
@Entity
@Table(name = "policy",
        indexes = @Index(name = "idx_policy_target", columnList = "resource,action"),
        uniqueConstraints = @UniqueConstraint(name = "uq_policy_name", columnNames = "name"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PolicyType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private PolicyEffect effect;

    /** Target resource this policy applies to (e.g. {@code transaction}). */
    @Column(nullable = false, length = 64)
    private String resource;

    /** Target action (e.g. {@code create}); {@code *} matches any action. */
    @Column(nullable = false, length = 32)
    private String action;

    /** JSON array of condition clauses; empty/absent means unconditional. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "condition_json")
    private String conditionJson;

    /** Lower numbers evaluate first; explicit DENY still overrides ALLOW on conflict. */
    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean enabled;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
