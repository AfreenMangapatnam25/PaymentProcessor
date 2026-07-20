package com.paymentprocessor.disputeservice.entity;

import com.paymentprocessor.disputeservice.domain.enums.DisputeStage;
import com.paymentprocessor.disputeservice.domain.enums.IssuerResponse;
import com.paymentprocessor.disputeservice.domain.enums.RepresentmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A challenge submitted to the card network on behalf of the merchant. A dispute
 * may have several across successive stages (initial representment,
 * pre-arbitration response, arbitration filing), but only one active submission
 * per stage.
 */
@Entity
@Table(name = "representments", indexes = {
        @Index(name = "idx_representment_dispute", columnList = "dispute_id")
})
public class Representment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "dispute_id", nullable = false)
    private String disputeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 30)
    private DisputeStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private RepresentmentStatus status = RepresentmentStatus.DRAFT;

    /** Tracking reference returned by the acquirer / network on submission. */
    @Column(name = "network_reference")
    private String networkReference;

    /** Number of evidence documents included in the submitted package. */
    @Column(name = "evidence_count")
    private int evidenceCount;

    /** Representment / arbitration filing fee in minor units. */
    @Column(name = "fee_minor")
    private Long feeMinor;

    @Enumerated(EnumType.STRING)
    @Column(name = "issuer_response", length = 20)
    private IssuerResponse issuerResponse;

    @Column(name = "narrative", length = 2000)
    private String narrative;

    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = "rpr_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisputeId() { return disputeId; }
    public void setDisputeId(String disputeId) { this.disputeId = disputeId; }
    public DisputeStage getStage() { return stage; }
    public void setStage(DisputeStage stage) { this.stage = stage; }
    public RepresentmentStatus getStatus() { return status; }
    public void setStatus(RepresentmentStatus status) { this.status = status; }
    public String getNetworkReference() { return networkReference; }
    public void setNetworkReference(String networkReference) { this.networkReference = networkReference; }
    public int getEvidenceCount() { return evidenceCount; }
    public void setEvidenceCount(int evidenceCount) { this.evidenceCount = evidenceCount; }
    public Long getFeeMinor() { return feeMinor; }
    public void setFeeMinor(Long feeMinor) { this.feeMinor = feeMinor; }
    public IssuerResponse getIssuerResponse() { return issuerResponse; }
    public void setIssuerResponse(IssuerResponse issuerResponse) { this.issuerResponse = issuerResponse; }
    public String getNarrative() { return narrative; }
    public void setNarrative(String narrative) { this.narrative = narrative; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
