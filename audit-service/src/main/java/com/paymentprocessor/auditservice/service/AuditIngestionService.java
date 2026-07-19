package com.paymentprocessor.auditservice.service;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.paymentprocessor.auditservice.common.Ulid;
import com.paymentprocessor.auditservice.config.AuditProperties;
import com.paymentprocessor.auditservice.domain.AuditRecord;
import com.paymentprocessor.auditservice.domain.ChainState;
import com.paymentprocessor.auditservice.repository.AuditRecordRepository;
import com.paymentprocessor.auditservice.repository.ChainStateStore;
import com.paymentprocessor.auditservice.service.exception.ChainIntegrityException;
import com.paymentprocessor.auditservice.service.exception.InvalidAuditEventException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Appends audit records to the tamper-evident chain.
 *
 * <p>Guarantees:
 * <ul>
 *   <li><b>Append-only</b> — records are inserted, never updated or deleted.</li>
 *   <li><b>Idempotent</b> — a repeated {@code eventId} returns the existing record.</li>
 *   <li><b>Consistently chained</b> — a unique {@code seq} index serialises concurrent
 *       appends; each record's {@code prevHash} is the previous record's {@code hash}.</li>
 *   <li><b>PII-free</b> — payloads are screened before persistence.</li>
 * </ul>
 *
 * <p>Concurrency protocol: readers all attempt {@code headSeq + 1}. The unique index on
 * {@code seq} lets exactly one writer win that slot; losers retry against the new head.
 * The record is inserted first, then the chain head is advanced with a compare-and-set,
 * so the head can never point past a record that was not actually written.
 */
@Service
public class AuditIngestionService {

    private static final Logger log = LoggerFactory.getLogger(AuditIngestionService.class);

    private final AuditRecordRepository records;
    private final ChainStateStore chainState;
    private final HashChainService hashChain;
    private final PiiGuardService piiGuard;
    private final String genesisHash;
    private final int maxRetries;

    private final Counter ingestedCounter;
    private final Counter dedupedCounter;

    public AuditIngestionService(AuditRecordRepository records,
                                 ChainStateStore chainState,
                                 HashChainService hashChain,
                                 PiiGuardService piiGuard,
                                 AuditProperties props,
                                 MeterRegistry meterRegistry) {
        this.records = records;
        this.chainState = chainState;
        this.hashChain = hashChain;
        this.piiGuard = piiGuard;
        this.genesisHash = props.getChain().getGenesisHash();
        this.maxRetries = props.getIngestion().getMaxRetriesOnContention();
        this.ingestedCounter = Counter.builder("audit.records.ingested").register(meterRegistry);
        this.dedupedCounter = Counter.builder("audit.records.deduped").register(meterRegistry);
    }

    /**
     * Appends a record. If an equal {@code eventId} was already stored, returns the
     * existing record without writing a new one (idempotent replay).
     */
    public AppendOutcome append(AuditAppendCommand cmd) {
        validate(cmd);
        piiGuard.assertRedacted(cmd.before(), cmd.after());

        // Fast-path idempotency check before doing any chain work.
        if (StringUtils.hasText(cmd.eventId())) {
            Optional<AuditRecord> existing = records.findByEventId(cmd.eventId());
            if (existing.isPresent()) {
                dedupedCounter.increment();
                return new AppendOutcome(existing.get(), false);
            }
        }

        DuplicateKeyException lastContention = null;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            ChainState head = chainState.getOrInitialise(genesisHash);
            long nextSeq = head.getSeq() + 1;

            AuditRecord record = buildRecord(cmd, nextSeq, head.getHeadHash());
            record.setHash(hashChain.computeHash(record));

            try {
                records.saveAndFlush(record);
            } catch (DuplicateKeyException dup) {
                // Either another writer took our seq, or this eventId already exists.
                if (StringUtils.hasText(cmd.eventId())) {
                    Optional<AuditRecord> existing = records.findByEventId(cmd.eventId());
                    if (existing.isPresent()) {
                        dedupedCounter.increment();
                        return new AppendOutcome(existing.get(), false);
                    }
                }
                lastContention = dup;
                continue; // seq contention — retry against the new head
            }

            advanceHead(head, nextSeq, record.getHash());
            ingestedCounter.increment();
            log.debug("Appended audit record id={} seq={} action={}",
                    record.getId(), record.getSeq(), record.getAction());
            return new AppendOutcome(record, true);
        }

        throw new ChainIntegrityException(
                "Could not append audit record after " + maxRetries + " attempts due to write "
                        + "contention on the chain head", lastContention);
    }

    private void advanceHead(ChainState observed, long newSeq, String newHead) {
        boolean advanced = chainState.compareAndAdvance(
                observed.getSeq(), observed.getHeadHash(), newSeq, newHead);
        if (advanced) {
            return;
        }
        // Because we exclusively own newSeq (unique index), the head could only have moved
        // if this exact advance already happened. Reconcile idempotently.
        ChainState current = chainState.getOrInitialise(genesisHash);
        if (current.getSeq() == newSeq && newHead.equals(current.getHeadHash())) {
            return;
        }
        throw new ChainIntegrityException(
                "Chain head advance failed unexpectedly: expected seq=" + observed.getSeq()
                        + " head=" + observed.getHeadHash() + ", current seq=" + current.getSeq());
    }

    private AuditRecord buildRecord(AuditAppendCommand cmd, long seq, String prevHash) {
        AuditRecord r = new AuditRecord();
        r.setId("aud_" + Ulid.generate());
        r.setSeq(seq);
        r.setPrevHash(prevHash);
        r.setRecordedAt(Instant.now());
        r.setTs(cmd.ts() != null ? cmd.ts() : r.getRecordedAt());
        r.setEventId(StringUtils.hasText(cmd.eventId()) ? cmd.eventId() : null);
        r.setActor(cmd.actor());
        r.setAction(cmd.action());
        r.setResource(cmd.resource());
        r.setMerchantId(cmd.merchantId());
        r.setBefore(cmd.before());
        r.setAfter(cmd.after());
        r.setRequestId(cmd.requestId());
        r.setTraceId(cmd.traceId());
        return r;
    }

    private void validate(AuditAppendCommand cmd) {
        if (cmd == null) {
            throw new InvalidAuditEventException("Event is null");
        }
        if (!StringUtils.hasText(cmd.action())) {
            throw new InvalidAuditEventException("action is required");
        }
        if (cmd.actor() == null || !StringUtils.hasText(cmd.actor().getType())) {
            throw new InvalidAuditEventException("actor.type is required");
        }
        if (cmd.resource() == null || !StringUtils.hasText(cmd.resource().getType())) {
            throw new InvalidAuditEventException("resource.type is required");
        }
    }
}
