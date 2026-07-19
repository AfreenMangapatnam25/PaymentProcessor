package com.paymentprocessor.authorization.repository;

import com.paymentprocessor.authorization.domain.enums.AuthorizationStatus;
import com.paymentprocessor.authorization.domain.payment.AuthorizationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthorizationRecordRepository extends JpaRepository<AuthorizationRecord, UUID> {

    Optional<AuthorizationRecord> findByIdempotencyKey(String idempotencyKey);

    Optional<AuthorizationRecord> findByGatewayAuthorizationId(String gatewayAuthorizationId);

    List<AuthorizationRecord> findByPaymentReference(String paymentReference);

    List<AuthorizationRecord> findByMerchantId(String merchantId);

    boolean existsByPaymentReferenceAndStatusIn(String paymentReference, List<AuthorizationStatus> statuses);

    @Query("""
            select a from AuthorizationRecord a
            where a.status in :statuses and a.expiresAt is not null and a.expiresAt < :now
            """)
    List<AuthorizationRecord> findExpirable(@Param("statuses") List<AuthorizationStatus> statuses,
                                            @Param("now") Instant now);

    @Modifying
    @Query("update AuthorizationRecord a set a.status = :status, a.updatedAt = :now where a.id = :id")
    int markStatus(@Param("id") UUID id,
                   @Param("status") AuthorizationStatus status,
                   @Param("now") Instant now);
}
