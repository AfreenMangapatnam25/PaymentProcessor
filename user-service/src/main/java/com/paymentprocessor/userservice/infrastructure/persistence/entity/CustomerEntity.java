package com.paymentprocessor.userservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * JPA mapping for {@code customers}. PII columns are ciphertext; email has a
 * per-merchant blind index. {@code deleted_at} drives soft delete, {@code
 * erased_at} the crypto-shred. Infrastructure-only (rule 1).
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
public class CustomerEntity extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 40)
    private String id;

    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    @Column(name = "user_id", length = 40)
    private String userId;

    @Column(name = "external_ref", length = 128)
    private String externalRef;

    @Column(name = "email_encrypted")
    private byte[] emailEncrypted;

    @Column(name = "email_index", length = 64)
    private String emailIndex;

    @Column(name = "full_name_encrypted")
    private byte[] fullNameEncrypted;

    @Column(name = "phone_encrypted")
    private byte[] phoneEncrypted;

    @Column(name = "phone_index", length = 64)
    private String phoneIndex;

    @Column(name = "default_instrument_token", length = 128)
    private String defaultInstrumentToken;

    @Column(name = "crypto_key_id", length = 40)
    private String cryptoKeyId;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
    private String metadata;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "erased_at")
    private Instant erasedAt;
}
