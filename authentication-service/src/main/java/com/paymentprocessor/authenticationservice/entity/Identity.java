package com.paymentprocessor.authenticationservice.entity;

import com.paymentprocessor.authenticationservice.domain.IdentityStatus;
import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "identities")
public class Identity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", length = 20, nullable = false)
    private PrincipalType principalType;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "phone_e164", length = 20)
    private String phoneE164;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private IdentityStatus status = IdentityStatus.PENDING;

    @Column(name = "mfa_required", nullable = false)
    private boolean mfaRequired = false;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = IdentityStatus.PENDING;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public PrincipalType getPrincipalType() { return principalType; }
    public void setPrincipalType(PrincipalType principalType) { this.principalType = principalType; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneE164() { return phoneE164; }
    public void setPhoneE164(String phoneE164) { this.phoneE164 = phoneE164; }
    public IdentityStatus getStatus() { return status; }
    public void setStatus(IdentityStatus status) { this.status = status; }
    public boolean isMfaRequired() { return mfaRequired; }
    public void setMfaRequired(boolean mfaRequired) { this.mfaRequired = mfaRequired; }
    public int getFailedLoginCount() { return failedLoginCount; }
    public void setFailedLoginCount(int failedLoginCount) { this.failedLoginCount = failedLoginCount; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(Instant emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }
    public Instant getPhoneVerifiedAt() { return phoneVerifiedAt; }
    public void setPhoneVerifiedAt(Instant phoneVerifiedAt) { this.phoneVerifiedAt = phoneVerifiedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
