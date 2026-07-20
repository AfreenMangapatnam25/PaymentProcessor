package com.paymentprocessor.merchantservice.repository;

import com.paymentprocessor.merchantservice.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessAddressRepository extends JpaRepository<BusinessAddress, UUID> {
    List<BusinessAddress> findByMerchantId(UUID merchantId);
    Optional<BusinessAddress> findByIdAndMerchantId(UUID id, UUID merchantId);
    List<BusinessAddress> findByMerchantIdAndPrimaryTrue(UUID merchantId);
}
