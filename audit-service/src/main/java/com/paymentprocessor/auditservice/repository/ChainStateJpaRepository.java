package com.paymentprocessor.auditservice.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.paymentprocessor.auditservice.domain.ChainState;

/**
 * Backing JPA repository for {@link ChainStateStore}. Not exposed directly to services —
 * {@link ChainStateStore} owns the compare-and-set semantics the hash chain relies on.
 */
interface ChainStateJpaRepository extends JpaRepository<ChainState, String> {

    @Modifying
    @Query("update ChainState c set c.seq = :newSeq, c.headHash = :newHead, c.updatedAt = :now "
            + "where c.id = :id and c.seq = :expectedSeq and c.headHash = :expectedHead")
    int compareAndAdvance(@Param("id") String id,
                          @Param("expectedSeq") long expectedSeq,
                          @Param("expectedHead") String expectedHead,
                          @Param("newSeq") long newSeq,
                          @Param("newHead") String newHead,
                          @Param("now") Instant now);
}
