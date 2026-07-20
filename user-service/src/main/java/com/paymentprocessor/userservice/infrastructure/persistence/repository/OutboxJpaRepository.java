package com.paymentprocessor.userservice.infrastructure.persistence.repository;

import com.paymentprocessor.userservice.infrastructure.persistence.entity.OutboxEventEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data access to {@code outbox_events}. The relay claims due rows with a
 * pessimistic write lock and SKIP LOCKED (Hibernate lock timeout -2), so many
 * relay replicas can drain the outbox concurrently without double-publishing
 * (rule 14).
 */
public interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select o from OutboxEventEntity o
            where o.status in ('PENDING', 'FAILED')
              and o.nextAttemptAt <= :now
            order by o.id asc
            """)
    List<OutboxEventEntity> findDueForUpdate(@Param("now") Instant now, Pageable pageable);
}
