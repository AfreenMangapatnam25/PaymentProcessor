package com.paymentprocessor.authenticationservice.repository;

import com.paymentprocessor.authenticationservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    /**
     * Claims a batch of undelivered events. {@code FOR UPDATE SKIP LOCKED} lets
     * multiple service instances run the relay concurrently without processing
     * the same row twice. Must be called inside a transaction.
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING'
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockPendingBatch(@Param("limit") int limit);
}
