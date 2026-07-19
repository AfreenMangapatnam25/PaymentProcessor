package com.paymentprocessor.settlementservice.repository;

import com.paymentprocessor.settlementservice.entity.SettlementBatch;
import com.paymentprocessor.settlementservice.enums.BatchStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, String> {

    List<SettlementBatch> findByMerchantId(String merchantId);

    List<SettlementBatch> findByStatus(BatchStatus status);

    List<SettlementBatch> findByStatusIn(Collection<BatchStatus> statuses);

    List<SettlementBatch> findByMerchantIdAndStatus(String merchantId, BatchStatus status);
}
