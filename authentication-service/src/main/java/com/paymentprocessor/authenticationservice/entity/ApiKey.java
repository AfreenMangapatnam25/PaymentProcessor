package com.paymentprocessor.authenticationservice.entity;

import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", length = 20, nullable = false)
    private PrincipalType ownerType;

    @Column(name = "owner_id", length = 36, nullable = false)
    private String ownerId;

    @Column(name = "prefix", length = 16, nullable = false, unique = true)
    private String prefix;

    @Column(name = "secret_hash", length = 64, nullable = false)
    private String secretHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "api_key_scopes", joinColumns = @JoinColumn(name = "api_key_id"))
    @Column(name = "scope", length = 64, nullable = false)
    private Set<String> scopes = new LinkedHashSet<>();

    @Column(name = "environment", length = 20, nullable = false)
    private String environment;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(now));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public PrincipalType getOwnerType() { return ownerType; }
    public void setOwnerType(PrincipalType ownerType) { this.ownerType = ownerType; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getSecretHash() { return secretHash; }
    public void setSecretHash(String secretHash) { this.secretHash = secretHash; }
    public Set<String> getScopes() { return scopes; }
    public void setScopes(Set<String> scopes) { this.scopes = scopes; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
