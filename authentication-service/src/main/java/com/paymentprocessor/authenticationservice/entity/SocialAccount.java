package com.paymentprocessor.authenticationservice.entity;

import com.paymentprocessor.authenticationservice.domain.SocialProvider;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Join record linking an external identity-provider account (Google/GitHub/Microsoft)
 * to a local {@link Identity}.
 *
 * <p>This is deliberately a separate table rather than columns on {@code identities},
 * because one local identity may be reachable through several providers (the same human
 * signing in with Google at work and GitHub at home must land on one account). The
 * unique constraint on {@code (provider, provider_user_id)} is what makes repeat logins
 * idempotent: the second and subsequent logins find this row instead of provisioning a
 * duplicate identity.
 *
 * <p>Note that no provider access/refresh tokens are persisted here. This service only
 * needs the provider to assert "who this is" once, at login; after that the caller
 * carries our own first-party JWT. Not storing third-party tokens keeps them out of the
 * breach blast radius.
 */
@Entity
@Table(
        name = "social_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_social_accounts_provider_user",
                columnNames = {"provider", "provider_user_id"}))
public class SocialAccount {

    /** Surrogate primary key (UUID string), assigned by the service layer. */
    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    /** The local identity this external account resolves to. */
    @Column(name = "identity_id", length = 36, nullable = false)
    private String identityId;

    /** Which external provider asserted this identity. */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20, nullable = false)
    private SocialProvider provider;

    /**
     * The provider's own immutable user identifier (OIDC {@code sub}, GitHub numeric id).
     * Never the email — emails get reassigned and changed, subject ids do not.
     */
    @Column(name = "provider_user_id", length = 255, nullable = false)
    private String providerUserId;

    /** Email as reported by the provider at the most recent login; may be null (GitHub). */
    @Column(name = "email", length = 320)
    private String email;

    /** Display name as reported by the provider at the most recent login; may be null. */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /** Avatar/picture URL as reported by the provider; may be null. */
    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    /** When this external account was first linked to the local identity. */
    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    /** Timestamp of the most recent successful login through this provider. */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Populates creation timestamps before the first insert. */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (linkedAt == null) {
            linkedAt = now;
        }
        if (lastLoginAt == null) {
            lastLoginAt = now;
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdentityId() { return identityId; }
    public void setIdentityId(String identityId) { this.identityId = identityId; }

    public SocialProvider getProvider() { return provider; }
    public void setProvider(SocialProvider provider) { this.provider = provider; }

    public String getProviderUserId() { return providerUserId; }
    public void setProviderUserId(String providerUserId) { this.providerUserId = providerUserId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Instant getLinkedAt() { return linkedAt; }
    public void setLinkedAt(Instant linkedAt) { this.linkedAt = linkedAt; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
