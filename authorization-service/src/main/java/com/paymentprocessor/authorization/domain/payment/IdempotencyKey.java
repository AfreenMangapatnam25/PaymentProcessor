package com.paymentprocessor.authorization.domain.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent idempotency guard. The first request under a given key records the payload hash and
 * the resulting authorization id; replays return the stored result, and conflicting payloads under
 * the same key are rejected.
 */
@Entity
@Table(name = "idempotency_key")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

    @Id
    @Column(name = "key_value", length = 128)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "resource_type", length = 64)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
