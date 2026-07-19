package com.paymentprocessor.authorization.repository;

import com.paymentprocessor.authorization.domain.payment.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    @Modifying
    @Query("delete from IdempotencyKey k where k.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
