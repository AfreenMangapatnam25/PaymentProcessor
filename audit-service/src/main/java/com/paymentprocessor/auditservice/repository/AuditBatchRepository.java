package com.paymentprocessor.auditservice.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.paymentprocessor.auditservice.domain.AuditBatch;

@Repository
public interface AuditBatchRepository extends JpaRepository<AuditBatch, String> {

    Optional<AuditBatch> findByBatchDate(LocalDate batchDate);

    boolean existsByBatchDate(LocalDate batchDate);
}
