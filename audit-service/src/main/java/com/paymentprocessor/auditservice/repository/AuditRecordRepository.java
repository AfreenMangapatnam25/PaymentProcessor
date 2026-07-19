package com.paymentprocessor.auditservice.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.paymentprocessor.auditservice.domain.AuditRecord;

/**
 * Read/append access to the audit records query copy.
 *
 * <p>Note: this interface deliberately exposes no update or delete operations. Audit
 * records are append-only; the service layer never mutates or removes them.
 */
@Repository
public interface AuditRecordRepository extends MongoRepository<AuditRecord, String> {

    Optional<AuditRecord> findByEventId(String eventId);

    Optional<AuditRecord> findBySeq(long seq);

    List<AuditRecord> findByMerchantIdOrderByTsDesc(String merchantId, Pageable pageable);

    List<AuditRecord> findByResourceTypeAndResourceIdOrderByTsDesc(
            String resourceType, String resourceId, Pageable pageable);

    List<AuditRecord> findByActionOrderByTsDesc(String action, Pageable pageable);

    /**
     * Ordered, inclusive slice of the chain by sequence — used for verification and
     * batching. Explicit GTE/LTE is used because Spring Data MongoDB's {@code Between}
     * keyword is exclusive on both bounds.
     */
    List<AuditRecord> findBySeqGreaterThanEqualAndSeqLessThanEqualOrderBySeqAsc(
            long fromInclusive, long toInclusive);

    /** Records with {@code recordedAt} in {@code [fromInclusive, toExclusive)}. */
    List<AuditRecord> findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderBySeqAsc(
            Instant fromInclusive, Instant toExclusive);

    @Query(value = "{ 'recordedAt': { $gte: ?0, $lt: ?1 } }", count = true)
    long countInRecordedWindow(Instant fromInclusive, Instant toExclusive);
}
