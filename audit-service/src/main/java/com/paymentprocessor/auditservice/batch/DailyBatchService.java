package com.paymentprocessor.auditservice.batch;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.auditservice.config.AuditProperties;
import com.paymentprocessor.auditservice.crypto.CanonicalJson;
import com.paymentprocessor.auditservice.crypto.MerkleTree;
import com.paymentprocessor.auditservice.domain.AuditBatch;
import com.paymentprocessor.auditservice.domain.AuditRecord;
import com.paymentprocessor.auditservice.repository.AuditBatchRepository;
import com.paymentprocessor.auditservice.repository.AuditRecordRepository;

/**
 * Seals one UTC day of audit records into the legal copy: a signed batch written to S3
 * with Object Lock, whose Merkle root is anchored externally.
 *
 * <p>The operation is idempotent per day (guarded by the batch id / unique date) and
 * transitions the batch manifest through SEALING → STORED → ANCHORED so partial failures
 * are visible and safely retryable.
 */
@Service
public class DailyBatchService {

    private static final Logger log = LoggerFactory.getLogger(DailyBatchService.class);

    private final AuditRecordRepository records;
    private final AuditBatchRepository batches;
    private final ObjectProvider<S3ObjectLockStore> s3StoreProvider;
    private final BatchSigner signer;
    private final ExternalAnchorService anchor;
    private final ObjectMapper objectMapper;
    private final AuditProperties.Batch batchConfig;
    private final AuditProperties.S3 s3Config;
    private final ZoneId zone;

    public DailyBatchService(AuditRecordRepository records,
                             AuditBatchRepository batches,
                             ObjectProvider<S3ObjectLockStore> s3StoreProvider,
                             BatchSigner signer,
                             ExternalAnchorService anchor,
                             ObjectMapper objectMapper,
                             AuditProperties props) {
        this.records = records;
        this.batches = batches;
        this.s3StoreProvider = s3StoreProvider;
        this.signer = signer;
        this.anchor = anchor;
        this.objectMapper = objectMapper;
        this.batchConfig = props.getBatch();
        this.s3Config = props.getS3();
        this.zone = ZoneId.of(props.getBatch().getZone());
    }

    /**
     * Seals the given UTC day. If a completed batch already exists for the day, it is
     * returned unchanged. Safe to re-invoke after a failure.
     */
    public AuditBatch sealDay(LocalDate day) {
        String batchId = "batch_" + day;

        AuditBatch existing = batches.findById(batchId).orElse(null);
        if (existing != null && existing.getStatus() == AuditBatch.Status.ANCHORED) {
            log.info("Batch {} already sealed and anchored; skipping.", batchId);
            return existing;
        }

        AuditBatch batch = existing != null ? existing : startManifest(batchId, day);

        Instant dayStart = day.atStartOfDay(zone).toInstant();
        Instant dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant();
        List<AuditRecord> dayRecords =
                records.findByRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderBySeqAsc(dayStart, dayEnd);

        byte[] content = buildContent(day, dayRecords);
        String rootHash = MerkleTree.computeRoot(dayRecords.stream().map(AuditRecord::getHash).toList());
        long fromSeq = dayRecords.isEmpty() ? 0 : dayRecords.get(0).getSeq();
        long toSeq = dayRecords.isEmpty() ? 0 : dayRecords.get(dayRecords.size() - 1).getSeq();

        batch.setFromSeq(fromSeq);
        batch.setToSeq(toSeq);
        batch.setRecordCount(dayRecords.size());
        batch.setRootHash(rootHash);

        String signingPayload = signingPayload(day, fromSeq, toSeq, dayRecords.size(), rootHash);
        batch.setSignature(signer.sign(signingPayload));
        batch.setSigningKeyId(signer.isEnabled() ? signer.getKeyId() : null);

        Instant retainUntil = Instant.now().plus(batchConfig.getRetentionYears() * 365L, ChronoUnit.DAYS);
        batch.setRetainUntil(retainUntil);

        storeToS3(batch, day, content, retainUntil);
        batch.setStatus(AuditBatch.Status.STORED);
        batches.save(batch);

        batch.setAnchorRef(anchor.anchor(day, rootHash));
        batch.setStatus(AuditBatch.Status.ANCHORED);
        batch.setSealedAt(Instant.now());
        batches.save(batch);

        log.info("Sealed batch {} records={} seq=[{}..{}] root={} anchor={}",
                batchId, dayRecords.size(), fromSeq, toSeq, rootHash, batch.getAnchorRef());
        return batch;
    }

    private void storeToS3(AuditBatch batch, LocalDate day, byte[] content, Instant retainUntil) {
        S3ObjectLockStore store = s3StoreProvider.getIfAvailable();
        if (store == null) {
            log.warn("S3 is disabled — batch {} not written to Object Lock storage. "
                    + "Enable audit.s3 in any real environment.", batch.getId());
            return;
        }
        String key = s3Key(day);
        S3ObjectLockStore.StoredObject stored =
                store.putImmutable(key, content, retainUntil, "application/json");
        batch.setS3Bucket(stored.bucket());
        batch.setS3Key(stored.key());
        batch.setS3VersionId(stored.versionId());
    }

    private AuditBatch startManifest(String batchId, LocalDate day) {
        AuditBatch batch = new AuditBatch();
        batch.setId(batchId);
        batch.setBatchDate(day);
        batch.setStatus(AuditBatch.Status.SEALING);
        batch.setCreatedAt(Instant.now());
        try {
            batches.insert(batch);
        } catch (DuplicateKeyException race) {
            return batches.findById(batchId).orElseThrow();
        }
        return batch;
    }

    /**
     * Builds the batch object content: a manifest header followed by the full records.
     * The stored bytes are exactly what the root hash and signature attest to.
     */
    private byte[] buildContent(LocalDate day, List<AuditRecord> dayRecords) {
        try {
            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("batchDate", day.toString());
            manifest.put("recordCount", dayRecords.size());
            manifest.put("schema", "audit-batch/v1");
            List<Object> recordNodes = new ArrayList<>(dayRecords.size());
            for (AuditRecord r : dayRecords) {
                recordNodes.add(objectMapper.convertValue(r, Map.class));
            }
            manifest.put("records", recordNodes);
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(manifest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize batch content", e);
        }
    }

    private String signingPayload(LocalDate day, long fromSeq, long toSeq, int count, String root) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("batchDate", day.toString());
        payload.put("fromSeq", fromSeq);
        payload.put("toSeq", toSeq);
        payload.put("recordCount", count);
        payload.put("root", root);
        return CanonicalJson.canonicalize(payload);
    }

    private String s3Key(LocalDate day) {
        return String.format("%s/%04d/%02d/batch_%s.json",
                s3Config.getKeyPrefix(), day.getYear(), day.getMonthValue(), day);
    }

    /** Convenience for the scheduler: the previous full UTC day. */
    public LocalDate previousDay() {
        return LocalDate.now(ZoneOffset.UTC).minusDays(1);
    }
}
