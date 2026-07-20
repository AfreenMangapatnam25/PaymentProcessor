package com.paymentprocessor.limit.repository;

import com.paymentprocessor.limit.domain.entity.UsageCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsageCounterRepository extends JpaRepository<UsageCounter, UUID> {

    Optional<UsageCounter> findByLimitConfigIdAndWindowKey(UUID limitConfigId, String windowKey);

    /**
     * Fetches the counter row under a pessimistic write lock (SELECT … FOR UPDATE)
     * so concurrent reserve/commit/release operations on the same limit + window
     * serialise and cannot double-spend capacity. Works across service instances
     * because the lock is held by the database.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from UsageCounter u
            where u.limitConfigId = :limitConfigId and u.windowKey = :windowKey
            """)
    Optional<UsageCounter> lockByLimitConfigIdAndWindowKey(@Param("limitConfigId") UUID limitConfigId,
                                                           @Param("windowKey") String windowKey);
}
