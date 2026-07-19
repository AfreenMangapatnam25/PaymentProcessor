package com.paymentprocessor.settlementservice.repository;

import com.paymentprocessor.settlementservice.entity.SettlementItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementItemRepository extends JpaRepository<SettlementItem, Long> {

    List<SettlementItem> findByBatchId(String batchId);

    /** Unbatched items eligible for aggregation for a merchant + currency. */
    List<SettlementItem> findByMerchantIdAndCurrencyAndBatchIdIsNull(String merchantId, String currency);

    /** Distinct (merchantId, currency) pairs that have unbatched items pending aggregation. */
    @org.springframework.data.jpa.repository.Query(
            "select distinct i.merchantId as merchantId, i.currency as currency "
                    + "from SettlementItem i where i.batchId is null")
    List<MerchantCurrency> findPendingMerchantCurrencies();

    boolean existsByIdempotencyKey(String idempotencyKey);

    /** Projection for the pending merchant/currency aggregation query. */
    interface MerchantCurrency {
        String getMerchantId();
        String getCurrency();
    }
}
