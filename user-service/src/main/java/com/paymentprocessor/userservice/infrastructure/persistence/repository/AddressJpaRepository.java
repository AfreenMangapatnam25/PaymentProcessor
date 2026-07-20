package com.paymentprocessor.userservice.infrastructure.persistence.repository;

import com.paymentprocessor.userservice.infrastructure.persistence.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data access to {@code addresses}. Owner-scoped, live-rows-only finders.
 */
public interface AddressJpaRepository extends JpaRepository<AddressEntity, String> {

    Optional<AddressEntity> findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
            String id, String ownerType, String ownerId);

    List<AddressEntity> findByOwnerTypeAndOwnerIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            String ownerType, String ownerId);

    Optional<AddressEntity> findByOwnerTypeAndOwnerIdAndAddressTypeAndIsDefaultTrueAndDeletedAtIsNull(
            String ownerType, String ownerId, String addressType);
}
