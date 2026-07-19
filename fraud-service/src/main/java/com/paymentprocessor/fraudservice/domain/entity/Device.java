package com.paymentprocessor.fraudservice.domain.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A device fingerprint observed for a merchant, used by velocity/device-risk
 * evaluators to detect new or repeat devices.
 */
@Entity
@Table(
        name = "devices",
        indexes = {
                @Index(name = "idx_devices_fingerprint", columnList = "fingerprint"),
                @Index(name = "idx_devices_merchant", columnList = "merchant_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String fingerprint;

    @Column(name = "merchant_id", length = 100)
    private String merchantId;

    @Column(name = "first_seen")
    private Instant firstSeen;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(columnDefinition = "TEXT")
    private String metadata;
}
