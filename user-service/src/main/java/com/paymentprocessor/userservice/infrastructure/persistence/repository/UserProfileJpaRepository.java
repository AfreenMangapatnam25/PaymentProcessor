package com.paymentprocessor.userservice.infrastructure.persistence.repository;

import com.paymentprocessor.userservice.infrastructure.persistence.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data access to {@code user_profiles}. The blind index lets us answer
 * "does this email exist?" without any plaintext.
 */
public interface UserProfileJpaRepository extends JpaRepository<UserProfileEntity, String> {

    boolean existsByEmailIndex(String emailIndex);
}
