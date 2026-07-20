package com.paymentprocessor.settlementservice.repository;

import com.paymentprocessor.settlementservice.entity.Adjustment;
import com.paymentprocessor.settlementservice.enums.AdjustmentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdjustmentRepository extends JpaRepository<Adjustment, String> {

    List<Adjustment> findByMerchantId(String merchantId);

    List<Adjustment> findByStatus(AdjustmentStatus status);

    List<Adjustment> findByMerchantIdAndStatus(String merchantId, AdjustmentStatus status);

    /** Approved-but-not-yet-applied adjustments for a merchant + currency, for inclusion in a batch. */
    List<Adjustment> findByMerchantIdAndCurrencyAndStatus(String merchantId, String currency, AdjustmentStatus status);
}
