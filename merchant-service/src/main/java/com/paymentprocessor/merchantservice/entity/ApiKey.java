package com.paymentprocessor.merchantservice.entity;

import com.paymentprocessor.merchantservice.common.enums.ApiKeyStatus;
import com.paymentprocessor.merchantservice.common.enums.ApiKeyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A merchant API credential. Only the public key id and a hash of the secret are stored; the
 * plaintext secret is shown exactly once at generation. Used by the security layer to
 * authenticate inbound requests.
 */
@Entity
@Table(name = "api_key", indexes = {
        @Index(name = "ux_apikey_key_id", columnList = "key_id", unique = true),
        @Index(name = "ix_apikey_merchant", columnList = "merchant_id")
})
@Getter
@Setter
public class ApiKey extends BaseEntity {

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    /** Public, non-secret identifier embedded in the presented key (e.g. "pk_live_..."). */
    @Column(name = "key_id", nullable = false, updatable = false, length = 60)
    private String keyId;

    /** Salted hash of the secret portion. */
    @Column(name = "secret_hash", nullable = false, length = 200)
    private String secretHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false, length = 20)
    private ApiKeyType keyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApiKeyStatus status = ApiKeyStatus.ACTIVE;

    @Column(name = "label", length = 120)
    private String label;

    /** Comma-separated CIDR/IP allowlist; empty means allow all. */
    @Column(name = "ip_allowlist", length = 1024)
    private String ipAllowlist;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
