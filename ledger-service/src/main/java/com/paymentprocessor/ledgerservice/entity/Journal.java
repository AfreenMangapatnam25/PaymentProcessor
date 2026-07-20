package com.paymentprocessor.ledgerservice.entity;

import com.paymentprocessor.ledgerservice.domain.enums.JournalStatus;
import com.paymentprocessor.ledgerservice.domain.enums.ReversalReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A journal: the atomic, balanced unit of financial recording. Once posted a
 * journal is immutable — corrections are made only by posting a compensating
 * reversal journal. The only fields mutated after posting are {@code status} and
 * {@code reversedByJournalId}, set when the journal is subsequently reversed.
 */
@Entity
@Table(name = "journals")
public class Journal {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "event_type", length = 64)
    private String eventType;

    @Column(name = "external_ref", length = 128)
    private String externalRef;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "metadata", length = 20000)
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private JournalStatus status = JournalStatus.POSTED;

    @Column(name = "reverses_journal_id", length = 64)
    private String reversesJournalId;

    @Column(name = "reversed_by_journal_id", length = 64)
    private String reversedByJournalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reversal_reason", length = 32)
    private ReversalReason reversalReason;

    @Column(name = "period_id", length = 64)
    private String periodId;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public JournalStatus getStatus() { return status; }
    public void setStatus(JournalStatus status) { this.status = status; }
    public String getReversesJournalId() { return reversesJournalId; }
    public void setReversesJournalId(String reversesJournalId) { this.reversesJournalId = reversesJournalId; }
    public String getReversedByJournalId() { return reversedByJournalId; }
    public void setReversedByJournalId(String reversedByJournalId) { this.reversedByJournalId = reversedByJournalId; }
    public ReversalReason getReversalReason() { return reversalReason; }
    public void setReversalReason(ReversalReason reversalReason) { this.reversalReason = reversalReason; }
    public String getPeriodId() { return periodId; }
    public void setPeriodId(String periodId) { this.periodId = periodId; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(Instant effectiveAt) { this.effectiveAt = effectiveAt; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public boolean isReversed() { return status == JournalStatus.REVERSED; }
    public boolean isReversal() { return reversesJournalId != null; }
}
