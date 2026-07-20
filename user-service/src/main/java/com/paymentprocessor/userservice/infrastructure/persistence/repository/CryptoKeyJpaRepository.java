package com.paymentprocessor.userservice.infrastructure.persistence.repository;

import com.paymentprocessor.userservice.infrastructure.persistence.entity.CryptoKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data access to {@code crypto_keys}. Infrastructure-internal.
 */
public interface CryptoKeyJpaRepository extends JpaRepository<CryptoKeyEntity, String> {

    Optional<CryptoKeyEntity> findByIdAndStatus(String id, String status);

    Optional<CryptoKeyEntity> findBySubjectTypeAndSubjectIdAndStatus(
            String subjectType, String subjectId, String status);
}
