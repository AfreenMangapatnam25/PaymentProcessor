package com.paymentprocessor.reconciliationservice.repository;

import com.paymentprocessor.reconciliationservice.domain.ExceptionRecord;
import com.paymentprocessor.reconciliationservice.domain.ExceptionStatus;
import com.paymentprocessor.reconciliationservice.domain.MismatchCategory;
import com.paymentprocessor.reconciliationservice.domain.ReviewQueue;
import com.paymentprocessor.reconciliationservice.domain.SeverityLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExceptionRecordRepository extends JpaRepository<ExceptionRecord, Long> {

    Optional<ExceptionRecord> findByUuid(UUID uuid);

    Page<ExceptionRecord> findByStatus(ExceptionStatus status, Pageable pageable);

    Page<ExceptionRecord> findByReconRunId(Long reconRunId, Pageable pageable);

    Page<ExceptionRecord> findBySeverityLevel(SeverityLevel severityLevel, Pageable pageable);

    Page<ExceptionRecord> findByReviewQueue(ReviewQueue reviewQueue, Pageable pageable);

    Page<ExceptionRecord> findByCategory(MismatchCategory category, Pageable pageable);

    long countByStatus(ExceptionStatus status);

    long countByReconRunIdAndStatus(Long reconRunId, ExceptionStatus status);
}
