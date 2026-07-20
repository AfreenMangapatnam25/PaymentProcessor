package com.paymentprocessor.merchantservice.repository;

import com.paymentprocessor.merchantservice.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KybCaseRepository extends JpaRepository<KybCase, UUID> {
    List<KybCase> findByMerchantId(UUID merchantId);
    Optional<KybCase> findByIdAndMerchantId(UUID id, UUID merchantId);
    Optional<KybCase> findFirstByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
}
