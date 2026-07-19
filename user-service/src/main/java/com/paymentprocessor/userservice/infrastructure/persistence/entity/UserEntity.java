package com.paymentprocessor.userservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA mapping for {@code users}. No PII columns (those live on
 * {@code user_profiles}); holds identity, status, the DEK reference, and the
 * erasure timestamp. Infrastructure-only (rule 1).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class UserEntity extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 40)
    private String id;

    @Column(name = "identity_id", nullable = false, length = 128)
    private String identityId;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "crypto_key_id", length = 40)
    private String cryptoKeyId;

    @Column(name = "erased_at")
    private Instant erasedAt;
}
