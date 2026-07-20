package com.paymentprocessor.authenticationservice.entity;

import com.paymentprocessor.authenticationservice.domain.MfaKind;
import com.paymentprocessor.authenticationservice.domain.MfaStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mfa_factors")
public class MfaFactor {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "identity_id", length = 36, nullable = false)
    private String identityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 20, nullable = false)
    private MfaKind kind;

    @Column(name = "secret_ref", length = 255)
    private String secretRef;

    @Column(name = "label", length = 120)
    private String label;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MfaStatus status = MfaStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIdentityId() { return identityId; }
    public void setIdentityId(String identityId) { this.identityId = identityId; }
    public MfaKind getKind() { return kind; }
    public void setKind(MfaKind kind) { this.kind = kind; }
    public String getSecretRef() { return secretRef; }
    public void setSecretRef(String secretRef) { this.secretRef = secretRef; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public MfaStatus getStatus() { return status; }
    public void setStatus(MfaStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
