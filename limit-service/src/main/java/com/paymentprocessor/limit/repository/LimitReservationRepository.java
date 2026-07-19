package com.paymentprocessor.limit.repository;

import com.paymentprocessor.limit.domain.entity.LimitReservation;
import com.paymentprocessor.limit.domain.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LimitReservationRepository extends JpaRepository<LimitReservation, UUID> {

    Optional<LimitReservation> findByIdempotencyKey(String idempotencyKey);

    List<LimitReservation> findByTransactionId(String transactionId);

    Page<LimitReservation> findByCustomerId(String customerId, Pageable pageable);

    /** Active reservations that have passed their expiry, for the sweeper job. */
    @Query("""
            select r from LimitReservation r
            where r.status = :status and r.expiresAt < :now
            order by r.expiresAt asc
            """)
    List<LimitReservation> findExpired(@Param("status") ReservationStatus status,
                                       @Param("now") Instant now,
                                       Pageable pageable);
}
