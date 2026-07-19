package com.paymentprocessor.userservice.domain.user;

import com.paymentprocessor.userservice.common.exception.ValidationException;
import com.paymentprocessor.userservice.domain.valueobject.IdentityId;
import com.paymentprocessor.userservice.domain.valueobject.UserId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * User aggregate root: a platform-level person. Holds identity (opaque
 * IdentityId), lifecycle status, and the PII-bearing {@link UserProfile}.
 *
 * <p>Business rules live here (rule 4): status transitions, and the fact that an
 * erased user is terminal and cannot be mutated. {@code version} carries the
 * optimistic-lock token (rule 8) so concurrent writers are detected at the
 * persistence boundary. Persistence/encryption concerns (DEK, ciphertext) are
 * deliberately absent -- the domain knows nothing about how it is stored.
 */
@Getter
@Builder(toBuilder = true)
public class User {

    private final UserId id;
    private final IdentityId identityId;
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    private UserProfile profile;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant erasedAt;
    @Builder.Default
    private long version = 0L;

    /**
     * Factory for a brand-new, active user with a mandatory profile.
     */
    public static User register(UserId id, IdentityId identityId, UserProfile profile, Instant now) {
        if (profile == null) {
            throw new ValidationException("profile", "A new user requires a profile");
        }
        return User.builder()
                .id(id)
                .identityId(identityId)
                .status(UserStatus.ACTIVE)
                .profile(profile)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();
    }

    public void activate() {
        assertNotErased();
        this.status = UserStatus.ACTIVE;
        touch();
    }

    public void suspend() {
        assertNotErased();
        this.status = UserStatus.SUSPENDED;
        touch();
    }

    public void lock() {
        assertNotErased();
        this.status = UserStatus.LOCKED;
        touch();
    }

    public void updateProfile(UserProfile profile) {
        assertNotErased();
        if (profile == null) {
            throw new ValidationException("profile", "Profile must not be null");
        }
        this.profile = profile;
        touch();
    }

    /**
     * Domain half of GDPR erasure: drop PII and move to the terminal ERASED
     * state. The irreversible part -- destroying the encryption key so the
     * ciphertext can never be read again -- is performed by infrastructure.
     */
    public void erase(Instant now) {
        assertNotErased();
        this.status = UserStatus.ERASED;
        this.profile = null;
        this.erasedAt = now;
        this.updatedAt = now;
    }

    public boolean isErased() {
        return status == UserStatus.ERASED;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    private void assertNotErased() {
        if (isErased()) {
            throw new ValidationException("status", "Erased users are immutable");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
