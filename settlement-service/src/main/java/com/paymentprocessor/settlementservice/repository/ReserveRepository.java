package com.paymentprocessor.settlementservice.repository;

import com.paymentprocessor.settlementservice.entity.Reserve;
import com.paymentprocessor.settlementservice.enums.ReserveStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReserveRepository extends JpaRepository<Reserve, String> {

    List<Reserve> findByMerchantId(String merchantId);

    List<Reserve> findByMerchantIdAndStatus(String merchantId, ReserveStatus status);

    List<Reserve> findByMerchantIdAndCurrencyAndStatus(String merchantId, String currency, ReserveStatus status);

    /** Reserves whose hold period has elapsed and are ready to release. */
    List<Reserve> findByStatusAndHoldUntilLessThanEqual(ReserveStatus status, LocalDate date);
}
