package com.paymentprocessor.merchantservice.repository;

import com.paymentprocessor.merchantservice.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KybDocumentRepository extends JpaRepository<KybDocument, UUID> {
    List<KybDocument> findByMerchantId(UUID merchantId);
    Optional<KybDocument> findByIdAndMerchantId(UUID id, UUID merchantId);
    List<KybDocument> findByKybCaseId(UUID kybCaseId);
}
