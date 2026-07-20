package com.paymentprocessor.limit.repository;

import com.paymentprocessor.limit.domain.entity.LimitAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LimitAuditLogRepository extends JpaRepository<LimitAuditLog, UUID> {

    Page<LimitAuditLog> findByEntityReference(String entityReference, Pageable pageable);

    Page<LimitAuditLog> findByTransactionId(String transactionId, Pageable pageable);
}
