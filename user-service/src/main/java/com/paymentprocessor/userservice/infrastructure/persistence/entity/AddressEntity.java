package com.paymentprocessor.userservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA mapping for {@code addresses}. Street lines are ciphertext (encrypted with
 * the owner's DEK, referenced by {@code crypto_key_id}); {@code country_code} is
 * clear. Polymorphic owner via {@code owner_type}/{@code owner_id} (no FK).
 * Infrastructure-only (rule 1).
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
public class AddressEntity extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 40)
    private String id;

    @Column(name = "owner_type", nullable = false, length = 16)
    private String ownerType;

    @Column(name = "owner_id", nullable = false, length = 40)
    private String ownerId;

    @Column(name = "address_type", nullable = false, length = 16)
    private String addressType;

    @Column(name = "line1_encrypted")
    private byte[] line1Encrypted;

    @Column(name = "line2_encrypted")
    private byte[] line2Encrypted;

    @Column(name = "city_encrypted")
    private byte[] cityEncrypted;

    @Column(name = "region_encrypted")
    private byte[] regionEncrypted;

    @Column(name = "postal_code_encrypted")
    private byte[] postalCodeEncrypted;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "crypto_key_id", length = 40)
    private String cryptoKeyId;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
