package com.paymentprocessor.authorization.domain.access;

import com.paymentprocessor.authorization.domain.enums.RoleScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Binding of a {@link Role} to an identity within a scope (global, merchant, or resource) and an
 * optional validity window.
 */
@Entity
@Table(name = "role_assignment",
        indexes = @Index(name = "idx_assignment_identity", columnList = "identity_id"),
        uniqueConstraints = @UniqueConstraint(
                name = "uq_assignment",
                columnNames = {"identity_id", "role_id", "scope", "scope_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignment {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "identity_id", nullable = false, length = 64)
    private String identityId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RoleScope scope;

    /** Merchant id or resource id the assignment is constrained to; null for GLOBAL scope. */
    @Column(name = "scope_id", length = 64)
    private String scopeId;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isActive(Instant at) {
        boolean afterStart = validFrom == null || !at.isBefore(validFrom);
        boolean beforeEnd = validUntil == null || at.isBefore(validUntil);
        return afterStart && beforeEnd;
    }
}
