package com.paymentprocessor.reconciliationservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A corrective financial adjustment posted to resolve an exception (e.g. a missing ledger entry,
 * a reversal, or an FX/fee variance). Adjustments above a configured threshold require dual
 * approval before they can be posted.
 */
@Entity
@Table(name = "recon_adjustment", indexes = {
        @Index(name = "idx_recon_adjustment_exception", columnList = "exception_id"),
        @Index(name = "idx_recon_adjustment_status", columnList = "status")
})
@Getter
@Setter
public class Adjustment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exception_id", nullable = false)
    private ExceptionRecord exceptionRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 24)
    private AdjustmentType adjustmentType;

    @Column(name = "amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "reason", nullable = false, length = 1024)
    private String reason;

    /** Reference to the ledger entry created by the Ledger Service once posted. */
    @Column(name = "ledger_reference", length = 64)
    private String ledgerReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AdjustmentStatus status = AdjustmentStatus.PENDING;

    @Column(name = "requires_dual_approval", nullable = false)
    private boolean requiresDualApproval = false;

    @Column(name = "created_by", nullable = false, length = 128)
    private String createdBy;

    @Column(name = "approved_by", length = 128)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "posted_at")
    private Instant postedAt;
}
