package com.paymentprocessor.userservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA mapping for {@code consents}. No PII, so no encryption. One row per
 * {@code (subject_type, subject_id, consent_kind)} (unique index).
 * Infrastructure-only (rule 1).
 */
@Entity
@Table(name = "consents")
@Getter
@Setter
public class ConsentEntity extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 40)
    private String id;

    @Column(name = "subject_type", nullable = false, length = 16)
    private String subjectType;

    @Column(name = "subject_id", nullable = false, length = 40)
    private String subjectId;

    @Column(name = "consent_kind", nullable = false, length = 32)
    private String consentKind;

    @Column(name = "granted", nullable = false)
    private boolean granted;

    @Column(name = "source", length = 64)
    private String source;

    @Column(name = "policy_version", length = 32)
    private String policyVersion;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
