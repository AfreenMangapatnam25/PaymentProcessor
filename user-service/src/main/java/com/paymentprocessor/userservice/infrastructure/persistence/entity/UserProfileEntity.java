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
 * JPA mapping for {@code user_profiles} (shared PK with {@code users}). All PII
 * columns are ciphertext ({@code *_encrypted}); {@code email_index}/{@code
 * phone_index} are the HMAC blind indexes. After crypto-shred these ciphertext
 * columns are nulled and the DEK destroyed, so the values are unrecoverable.
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
public class UserProfileEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false, length = 40)
    private String userId;

    @Column(name = "email_encrypted")
    private byte[] emailEncrypted;

    @Column(name = "email_index", length = 64)
    private String emailIndex;

    @Column(name = "first_name_encrypted")
    private byte[] firstNameEncrypted;

    @Column(name = "last_name_encrypted")
    private byte[] lastNameEncrypted;

    @Column(name = "phone_encrypted")
    private byte[] phoneEncrypted;

    @Column(name = "phone_index", length = 64)
    private String phoneIndex;

    @Column(name = "date_of_birth_encrypted")
    private byte[] dateOfBirthEncrypted;

    @Column(name = "locale", length = 16)
    private String locale;

    @Column(name = "timezone", length = 64)
    private String timezone;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
