package com.paymentprocessor.disputeservice.entity;

import com.paymentprocessor.disputeservice.domain.enums.DisputeStatus;
import com.paymentprocessor.disputeservice.domain.enums.TimelineEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An immutable entry on a dispute's status / audit timeline. Every meaningful
 * action — status change, notification, evidence upload, ledger posting — is
 * recorded here to provide the complete audit trail required for compliance.
 */
@Entity
@Table(name = "dispute_events", indexes = {
        @Index(name = "idx_event_dispute", columnList = "dispute_id")
})
public class DisputeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispute_id", nullable = false)
    private String disputeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private TimelineEventType type;

    /** Who or what triggered the event (SYSTEM, MERCHANT, PLATFORM, NETWORK). */
    @Column(name = "actor", length = 40)
    private String actor;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private DisputeStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 30)
    private DisputeStatus toStatus;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDisputeId() { return disputeId; }
    public void setDisputeId(String disputeId) { this.disputeId = disputeId; }
    public TimelineEventType getType() { return type; }
    public void setType(TimelineEventType type) { this.type = type; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DisputeStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(DisputeStatus fromStatus) { this.fromStatus = fromStatus; }
    public DisputeStatus getToStatus() { return toStatus; }
    public void setToStatus(DisputeStatus toStatus) { this.toStatus = toStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
