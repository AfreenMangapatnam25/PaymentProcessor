package com.paymentprocessor.reconciliationservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single reconciliation job execution against one counterparty/channel for a business date.
 * Aggregates the outcome counters produced by the matching engine.
 */
@Entity
@Table(name = "recon_run", indexes = {
        @Index(name = "idx_recon_run_uuid", columnList = "uuid", unique = true),
        @Index(name = "idx_recon_run_type_date", columnList = "recon_type, business_date"),
        @Index(name = "idx_recon_run_status", columnList = "status")
})
@Getter
@Setter
public class ReconRun extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(name = "recon_type", nullable = false, length = 32)
    private ReconType reconType;

    /** Counterparty / channel identifier, e.g. "VISA", "STRIPE", "ACME_BANK". */
    @Column(name = "channel", nullable = false, length = 128)
    private String channel;

    @Column(name = "account_ref", length = 128)
    private String accountRef;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "currency", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ReconRunStatus status = ReconRunStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "total_internal", nullable = false)
    private long totalInternal = 0;

    @Column(name = "total_external", nullable = false)
    private long totalExternal = 0;

    @Column(name = "matched_count", nullable = false)
    private long matchedCount = 0;

    @Column(name = "mismatched_count", nullable = false)
    private long mismatchedCount = 0;

    @Column(name = "missing_internal_count", nullable = false)
    private long missingInternalCount = 0;

    @Column(name = "missing_external_count", nullable = false)
    private long missingExternalCount = 0;

    @Column(name = "duplicate_count", nullable = false)
    private long duplicateCount = 0;

    @Column(name = "exception_count", nullable = false)
    private long exceptionCount = 0;

    @Column(name = "matched_amount", precision = 20, scale = 4)
    private BigDecimal matchedAmount = BigDecimal.ZERO;

    @Column(name = "triggered_by", length = 128)
    private String triggeredBy;

    @Column(name = "failure_reason", length = 1024)
    private String failureReason;

    /** Auto-match rate as a fraction of total matched candidates (0-1). */
    @Column(name = "match_rate", precision = 6, scale = 4)
    private BigDecimal matchRate;
}
