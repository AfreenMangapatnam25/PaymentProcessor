package com.paymentprocessor.reconciliationservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A confirmed link between an internal and an external {@link ReconRecord}, produced either by the
 * automatic matching engine or by a manual analyst action.
 */
@Entity
@Table(name = "recon_match", indexes = {
        @Index(name = "idx_recon_match_run", columnList = "recon_run_id"),
        @Index(name = "idx_recon_match_internal", columnList = "internal_record_id"),
        @Index(name = "idx_recon_match_external", columnList = "external_record_id")
})
@Getter
@Setter
public class MatchRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recon_run_id", nullable = false)
    private ReconRun reconRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internal_record_id", nullable = false)
    private ReconRecord internalRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "external_record_id", nullable = false)
    private ReconRecord externalRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 16)
    private MatchType matchType;

    /** Name of the rule that produced the match, e.g. "AMOUNT_AND_REFERENCE". */
    @Column(name = "match_rule", nullable = false, length = 64)
    private String matchRule;

    /** Rule confidence in the range 0-1. */
    @Column(name = "confidence", nullable = false, precision = 6, scale = 4)
    private BigDecimal confidence;

    @Column(name = "amount_variance", precision = 20, scale = 4)
    private BigDecimal amountVariance;

    @Column(name = "date_variance_days")
    private Integer dateVarianceDays;

    @Column(name = "note", length = 512)
    private String note;

    /** True when the match was created by a human analyst rather than the engine. */
    @Column(name = "manual", nullable = false)
    private boolean manual = false;

    @Column(name = "matched_at", nullable = false)
    private Instant matchedAt = Instant.now();
}
