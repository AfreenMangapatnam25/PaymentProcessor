package com.paymentprocessor.authenticationservice.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mfa_recovery_codes")
public class MfaRecoveryCode {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "identity_id", length = 36, nullable = false)
    private String identityId;

    @Column(name = "code_hash", length = 64, nullable = false)
    private String codeHash;

    @Column(name = "used_at")
    private Instant usedAt;

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
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
