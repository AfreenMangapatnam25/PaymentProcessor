package com.paymentprocessor.userservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA mapping for {@code crypto_keys}. Never leaves the infrastructure layer
 * (rule 1). Holds only the WRAPPED DEK; the plaintext key exists solely in
 * memory during a request.
 */
@Entity
@Table(name = "crypto_keys")
@Getter
@Setter
public class CryptoKeyEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 40)
    private String id;

    @Column(name = "subject_type", nullable = false, length = 16)
    private String subjectType;

    @Column(name = "subject_id", nullable = false, length = 40)
    private String subjectId;

    @Column(name = "wrapped_dek")
    private byte[] wrappedDek;

    @Column(name = "key_version", nullable = false)
    private int keyVersion;

    @Column(name = "algorithm", nullable = false, length = 32)
    private String algorithm;

    @Column(name = "kms_master_key_id", nullable = false, length = 256)
    private String kmsMasterKeyId;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "destroyed_at")
    private Instant destroyedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
