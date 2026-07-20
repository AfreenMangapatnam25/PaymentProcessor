package com.paymentprocessor.merchantservice.repository;

import com.paymentprocessor.merchantservice.common.enums.PaymentMethodType;
import com.paymentprocessor.merchantservice.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodConfigRepository extends JpaRepository<PaymentMethodConfig, UUID> {
    List<PaymentMethodConfig> findByMerchantId(UUID merchantId);
    Optional<PaymentMethodConfig> findByMerchantIdAndMethodType(UUID merchantId, PaymentMethodType methodType);
}
