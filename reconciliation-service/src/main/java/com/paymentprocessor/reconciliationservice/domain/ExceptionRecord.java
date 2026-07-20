package com.paymentprocessor.reconciliationservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A reconciliation exception (mismatch case) requiring resolution. Produced by mismatch detection
 * when records cannot be auto-matched within tolerance, and tracked through to resolution with a
 * full audit trail.
 */
@Entity
@Table(name = "recon_exception", indexes = {
        @Index(name = "idx_recon_exception_uuid", columnList = "uuid", unique = true),
        @Index(name = "idx_recon_exception_run", columnList = "recon_run_id"),
        @Index(name = "idx_recon_exception_status", columnList = "status"),
        @Index(name = "idx_recon_exception_severity", columnList = "severity_level"),
        @Index(name = "idx_recon_exception_queue", columnList = "review_queue")
})
@Getter
@Setter
public class ExceptionRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recon_run_id", nullable = false)
    private ReconRun reconRun;

    /** The offending record, when the exception is tied to a single side. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recon_record_id")
    private ReconRecord reconRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private MismatchCategory category;

    @Column(name = "severity_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal severityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_level", nullable = false, length = 16)
    private SeverityLevel severityLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ExceptionStatus status = ExceptionStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_queue", length = 32)
    private ReviewQueue reviewQueue;

    @Column(name = "amount", precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "expected_amount", precision = 20, scale = 4)
    private BigDecimal expectedAmount;

    @Column(name = "actual_amount", precision = 20, scale = 4)
    private BigDecimal actualAmount;

    @Column(name = "external_reference", length = 128)
    private String externalReference;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "age_days", nullable = false)
    private int ageDays = 0;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt = Instant.now();

    @Column(name = "sla_due_at")
    private Instant slaDueAt;

    @Column(name = "assigned_to", length = 128)
    private String assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type", length = 24)
    private ResolutionType resolutionType;

    @Column(name = "resolution_note", length = 1024)
    private String resolutionNote;

    @Column(name = "resolved_by", length = 128)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /** True when the exception was created and resolved automatically by the engine. */
    @Column(name = "auto_resolved", nullable = false)
    private boolean autoResolved = false;
}
