package com.paymentprocessor.settlementservice.repository;

import com.paymentprocessor.settlementservice.entity.Payout;
import com.paymentprocessor.settlementservice.enums.PayoutStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, String> {

    List<Payout> findByBatchId(String batchId);

    List<Payout> findByMerchantId(String merchantId);

    List<Payout> findByStatus(PayoutStatus status);

    List<Payout> findByStatusIn(Collection<PayoutStatus> statuses);

    Optional<Payout> findByIdempotencyKey(String idempotencyKey);

    /** Payouts that are due for a retry attempt now. */
    List<Payout> findByStatusAndNextRetryAtLessThanEqual(PayoutStatus status, Instant now);
}
