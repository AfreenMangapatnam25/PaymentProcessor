package com.paymentprocessor.auditservice.domain;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * An immutable audit record — the query copy held in MongoDB.
 *
 * <p>Records are append-only: once written they are never updated or deleted. Each
 * record carries a strictly increasing {@code seq} and is hash-chained to its
 * predecessor via {@code prevHash}/{@code hash}, so any tampering (including deletion
 * or reordering) is detectable.
 *
 * <p>{@code before}/{@code after} hold redacted diffs only — references and masked
 * values, never raw PII — which is what keeps these records exempt from GDPR erasure.
 */
@Document(collection = "audit_records")
public class AuditRecord {

    /** Public identifier, e.g. {@code aud_01J...} (ULID-based, lexicographically sortable). */
    @Id
    private String id;

    /**
     * Strictly increasing position in the global hash chain. Unique — this is the
     * mutual-exclusion guard that serialises concurrent appends.
     */
    private long seq;

    /** Business timestamp of the event (when it happened at the source). */
    private Instant ts;

    /** Server-side receive time (when this service persisted the record). */
    private Instant recordedAt;

    private Actor actor;
    private String action;
    private ResourceRef resource;

    @Field("merchant_id")
    private String merchantId;

    /** Redacted prior state. Never raw PII. */
    private Map<String, Object> before;

    /** Redacted new state. Never raw PII. */
    private Map<String, Object> after;

    @Field("request_id")
    private String requestId;

    @Field("trace_id")
    private String traceId;

    /**
     * Idempotency / dedup key supplied by the producer (unique). Two deliveries of the
     * same logical event collapse to a single record.
     */
    @Field("event_id")
    private String eventId;

    @Field("prev_hash")
    private String prevHash;

    /** {@code sha256:...} over the canonical form of this record plus {@code prevHash}. */
    private String hash;

    /** Set once the record has been sealed into a daily S3 batch; null until then. */
    @Field("batch_id")
    private String batchId;

    public AuditRecord() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getSeq() { return seq; }
    public void setSeq(long seq) { this.seq = seq; }
    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
    public Actor getActor() { return actor; }
    public void setActor(Actor actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public ResourceRef getResource() { return resource; }
    public void setResource(ResourceRef resource) { this.resource = resource; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public Map<String, Object> getBefore() { return before; }
    public void setBefore(Map<String, Object> before) { this.before = before; }
    public Map<String, Object> getAfter() { return after; }
    public void setAfter(Map<String, Object> after) { this.after = after; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
}
