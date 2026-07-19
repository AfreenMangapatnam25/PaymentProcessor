package com.paymentprocessor.merchantservice.repository;

import com.paymentprocessor.merchantservice.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BeneficialOwnerRepository extends JpaRepository<BeneficialOwner, UUID> {
    List<BeneficialOwner> findByMerchantId(UUID merchantId);
    Optional<BeneficialOwner> findByIdAndMerchantId(UUID id, UUID merchantId);
    long countByMerchantId(UUID merchantId);
}
