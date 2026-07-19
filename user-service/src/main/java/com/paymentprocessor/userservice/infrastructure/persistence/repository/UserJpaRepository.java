package com.paymentprocessor.userservice.infrastructure.persistence.repository;

import com.paymentprocessor.userservice.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data access to {@code users}. Infrastructure-internal; the domain talks
 * to {@code UserRepository} (the port), whose adapter delegates here.
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByIdentityId(String identityId);

    boolean existsByIdentityId(String identityId);
}
