package com.paymentprocessor.authenticationservice.entity;

import com.paymentprocessor.authenticationservice.domain.CredentialKind;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "credentials")
public class Credential {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "identity_id", length = 36, nullable = false)
    private String identityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 20, nullable = false)
    private CredentialKind kind = CredentialKind.PASSWORD;

    @Column(name = "secret_hash", length = 255, nullable = false)
    private String secretHash;

    @Column(name = "algo_params", length = 100)
    private String algoParams;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

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
    public CredentialKind getKind() { return kind; }
    public void setKind(CredentialKind kind) { this.kind = kind; }
    public String getSecretHash() { return secretHash; }
    public void setSecretHash(String secretHash) { this.secretHash = secretHash; }
    public String getAlgoParams() { return algoParams; }
    public void setAlgoParams(String algoParams) { this.algoParams = algoParams; }
    public Instant getRotatedAt() { return rotatedAt; }
    public void setRotatedAt(Instant rotatedAt) { this.rotatedAt = rotatedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
