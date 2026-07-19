package com.paymentprocessor.auditservice.domain;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Metadata for a sealed daily batch — the legal copy written to S3 with Object Lock.
 * The batch content itself lives in S3; this document is the queryable manifest.
 */
@Document(collection = "audit_batches")
public class AuditBatch {

    public enum Status { SEALING, STORED, ANCHORED, FAILED }

    /** Batch id, e.g. {@code batch_2026-07-16}. */
    @Id
    private String id;

    /** UTC day the batch seals. */
    private LocalDate batchDate;

    private long fromSeq;
    private long toSeq;
    private long recordCount;

    /** Merkle root over the record hashes, {@code sha256:...}. Empty batches have a sentinel. */
    private String rootHash;

    /** Base64 signature over the signing payload (root + range + date). */
    private String signature;
    private String signingKeyId;

    @Field("s3_bucket")
    private String s3Bucket;
    @Field("s3_key")
    private String s3Key;
    @Field("s3_version_id")
    private String s3VersionId;

    /** Object Lock retain-until instant (COMPLIANCE mode). */
    private Instant retainUntil;

    /** Reference returned by the external anchoring service. */
    private String anchorRef;

    private Status status;
    private Instant createdAt;
    private Instant sealedAt;

    public AuditBatch() {
    }

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
}
