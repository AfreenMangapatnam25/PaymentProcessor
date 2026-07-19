package com.paymentprocessor.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "suppressions")
public class Suppression {

    @EmbeddedId
    private SuppressionId id;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Suppression() {
    }

    public Suppression(String channel, byte[] recipientHash, String reason) {
        this.id = new SuppressionId(channel, recipientHash);
        this.reason = reason;
    }

    public SuppressionId getId() { return id; }
    public void setId(SuppressionId id) { this.id = id; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getChannel() { return id == null ? null : id.getChannel(); }
    public byte[] getRecipientHash() { return id == null ? null : id.getRecipientHash(); }
}
