package com.paymentprocessor.auditservice.domain;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Metadata for a sealed daily batch — the legal copy written to S3 with Object Lock.
 * The batch content itself lives in S3; this row is the queryable manifest.
 *
 * <p>Implements {@link Persistable} for the same reason as {@link AuditRecord}: the id
 * is client-assigned ({@code batch_<date>}), so without it Spring Data JPA would merge
 * instead of insert, masking the idempotent-create race in {@code DailyBatchService}.
 */
@Entity
@Table(name = "audit_batches")
public class AuditBatch implements Persistable<String> {

    public enum Status { SEALING, STORED, ANCHORED, FAILED }

    /** Batch id, e.g. {@code batch_2026-07-16}. */
    @Id
    private String id;

    /** UTC day the batch seals. */
    @Column(name = "batch_date")
    private LocalDate batchDate;

    @Column(name = "from_seq")
    private long fromSeq;
    @Column(name = "to_seq")
    private long toSeq;
    @Column(name = "record_count")
    private long recordCount;

    /** Merkle root over the record hashes, {@code sha256:...}. Empty batches have a sentinel. */
    @Column(name = "root_hash")
    private String rootHash;

    /** Base64 signature over the signing payload (root + range + date). */
    private String signature;
    @Column(name = "signing_key_id")
    private String signingKeyId;

    @Column(name = "s3_bucket")
    private String s3Bucket;
    @Column(name = "s3_key")
    private String s3Key;
    @Column(name = "s3_version_id")
    private String s3VersionId;

    /** Object Lock retain-until instant (COMPLIANCE mode). */
    @Column(name = "retain_until")
    private Instant retainUntil;

    /** Reference returned by the external anchoring service. */
    @Column(name = "anchor_ref")
    private String anchorRef;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "sealed_at")
    private Instant sealedAt;

    @Transient
    private transient boolean isNew = true;

    public AuditBatch() {
    }

    @Override
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public LocalDate getBatchDate() { return batchDate; }
    public void setBatchDate(LocalDate batchDate) { this.batchDate = batchDate; }
    public long getFromSeq() { return fromSeq; }
    public void setFromSeq(long fromSeq) { this.fromSeq = fromSeq; }
    public long getToSeq() { return toSeq; }
    public void setToSeq(long toSeq) { this.toSeq = toSeq; }
    public long getRecordCount() { return recordCount; }
    public void setRecordCount(long recordCount) { this.recordCount = recordCount; }
    public String getRootHash() { return rootHash; }
    public void setRootHash(String rootHash) { this.rootHash = rootHash; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public String getSigningKeyId() { return signingKeyId; }
    public void setSigningKeyId(String signingKeyId) { this.signingKeyId = signingKeyId; }
    public String getS3Bucket() { return s3Bucket; }
    public void setS3Bucket(String s3Bucket) { this.s3Bucket = s3Bucket; }
    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public String getS3VersionId() { return s3VersionId; }
    public void setS3VersionId(String s3VersionId) { this.s3VersionId = s3VersionId; }
    public Instant getRetainUntil() { return retainUntil; }
    public void setRetainUntil(Instant retainUntil) { this.retainUntil = retainUntil; }
    public String getAnchorRef() { return anchorRef; }
    public void setAnchorRef(String anchorRef) { this.anchorRef = anchorRef; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getSealedAt() { return sealedAt; }
    public void setSealedAt(Instant sealedAt) { this.sealedAt = sealedAt; }

    @Override
    public boolean isNew() { return isNew; }

    @PostPersist
    @PostLoad
    void markNotNew() { this.isNew = false; }
}
