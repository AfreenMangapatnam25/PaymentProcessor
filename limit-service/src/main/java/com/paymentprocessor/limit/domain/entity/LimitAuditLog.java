package com.paymentprocessor.limit.domain.entity;

import com.paymentprocessor.limit.domain.enums.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit trail entry for limit configuration changes and limit
 * consumption, retained for compliance reporting and dispute evidence.
 */
@Entity
@Table(
        name = "limit_audit_log",
        indexes = {
                @Index(name = "idx_audit_entity", columnList = "entity_reference"),
                @Index(name = "idx_audit_txn", columnList = "transaction_id"),
                @Index(name = "idx_audit_created", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    /** Scope reference the action concerns (customer/merchant/config id). */
    @Column(name = "entity_reference", length = 150)
    private String entityReference;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    /** Actor that triggered the action (service name or admin user). */
    @Column(length = 100)
    private String actor;

    /** JSON snapshot of the relevant detail (before/after, decision reasons, …). */
    @Column(name = "detail", columnDefinition = "text")
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
