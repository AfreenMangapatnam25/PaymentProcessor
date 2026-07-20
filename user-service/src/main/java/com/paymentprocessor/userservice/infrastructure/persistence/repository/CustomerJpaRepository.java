package com.paymentprocessor.userservice.infrastructure.persistence.repository;

import com.paymentprocessor.userservice.infrastructure.persistence.entity.CustomerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data access to {@code customers}. Every finder is merchant-scoped and,
 * for the normal read paths, excludes soft-deleted rows -- so isolation and
 * soft delete are enforced in the query, not left to the caller.
 */
public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, String> {

    Optional<CustomerEntity> findByIdAndMerchantIdAndDeletedAtIsNull(String id, String merchantId);

    Optional<CustomerEntity> findByIdAndMerchantId(String id, String merchantId);

    Page<CustomerEntity> findByMerchantIdAndDeletedAtIsNull(String merchantId, Pageable pageable);

    long countByMerchantIdAndDeletedAtIsNull(String merchantId);

    boolean existsByMerchantIdAndExternalRefAndDeletedAtIsNull(String merchantId, String externalRef);

    boolean existsByMerchantIdAndEmailIndexAndDeletedAtIsNull(String merchantId, String emailIndex);
}
