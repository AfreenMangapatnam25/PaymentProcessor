package com.paymentprocessor.authenticationservice.entity;

import com.paymentprocessor.authenticationservice.domain.DeviceTrust;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "identity_id", length = 36, nullable = false)
    private String identityId;

    @Column(name = "fingerprint", length = 128, nullable = false)
    private String fingerprint;

    @Column(name = "label", length = 120)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_level", length = 20, nullable = false)
    private DeviceTrust trustLevel = DeviceTrust.UNKNOWN;

    @Column(name = "last_ip", length = 45)
    private String lastIp;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIdentityId() { return identityId; }
    public void setIdentityId(String identityId) { this.identityId = identityId; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public DeviceTrust getTrustLevel() { return trustLevel; }
    public void setTrustLevel(DeviceTrust trustLevel) { this.trustLevel = trustLevel; }
    public String getLastIp() { return lastIp; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
