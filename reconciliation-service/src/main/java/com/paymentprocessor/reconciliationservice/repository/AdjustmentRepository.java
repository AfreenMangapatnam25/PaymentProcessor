package com.paymentprocessor.reconciliationservice.repository;

import com.paymentprocessor.reconciliationservice.domain.Adjustment;
import com.paymentprocessor.reconciliationservice.domain.AdjustmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdjustmentRepository extends JpaRepository<Adjustment, Long> {

    List<Adjustment> findByExceptionRecordId(Long exceptionRecordId);

    Page<Adjustment> findByStatus(AdjustmentStatus status, Pageable pageable);
}
