package com.paymentprocessor.userservice.domain.repository;

import com.paymentprocessor.userservice.domain.user.User;
import com.paymentprocessor.userservice.domain.valueobject.Email;
import com.paymentprocessor.userservice.domain.valueobject.IdentityId;
import com.paymentprocessor.userservice.domain.valueobject.UserId;

import java.util.Optional;

/**
 * Domain port for user persistence (hexagonal). Speaks ONLY in domain types --
 * no JPA, no entities (rule 1). The infrastructure layer provides an adapter
 * that maps to/from {@code UserEntity}, handles envelope encryption, and
 * enforces optimistic locking. Application services depend on this interface,
 * never on the adapter.
 */
public interface UserRepository {

    /** Insert or update; returns the persisted aggregate (with bumped version). */
    User save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByIdentityId(IdentityId identityId);

    boolean existsByIdentityId(IdentityId identityId);

    /**
     * Existence check by email using the deterministic blind index, so no
     * plaintext email is required and no ciphertext is scanned.
     */
    boolean existsByEmail(Email email);
}
