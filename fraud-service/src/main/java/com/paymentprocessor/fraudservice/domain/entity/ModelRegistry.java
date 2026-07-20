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
 * Metadata for a deployed/deployable ML scoring model version.
 */
@Entity
@Table(
        name = "model_registry",
        indexes = {
                @Index(name = "idx_model_registry_name", columnList = "name"),
                @Index(name = "idx_model_registry_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 50)
    private String version;

    @Column(length = 20)
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;
}
