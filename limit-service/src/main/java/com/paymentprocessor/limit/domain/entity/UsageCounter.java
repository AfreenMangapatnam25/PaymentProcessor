package com.paymentprocessor.limit.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Real-time consumption of a single {@link LimitConfiguration} inside one window
 * instance (identified by {@code windowKey}, e.g. "2026-07-18" for a daily limit).
 *
 * <p>Usage is split into <b>reserved</b> (held during processing, not yet captured)
 * and <b>committed</b> (finalised). The effective consumption checked against the
 * threshold is {@code reserved + committed}. Rows are locked with a pessimistic
 * write lock during reserve/commit/release so concurrent transactions cannot
 * double-spend the same capacity.
 */
@Entity
@Table(
        name = "usage_counter",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_usage_counter",
                columnNames = {"limit_config_id", "window_key"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "limit_config_id", nullable = false)
    private UUID limitConfigId;

    /** Window instance identifier — e.g. "2026-07-18", "2026-W29", "2026-07", "ALL". */
    @Column(name = "window_key", nullable = false, length = 40)
    private String windowKey;

    @Column(name = "reserved_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal reservedAmount = BigDecimal.ZERO;

    @Column(name = "committed_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal committedAmount = BigDecimal.ZERO;

    @Column(name = "reserved_count", nullable = false)
    @Builder.Default
    private long reservedCount = 0L;

    @Column(name = "committed_count", nullable = false)
    @Builder.Default
    private long committedCount = 0L;

    /** Start of this window instance, used for housekeeping / archival. */
    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Version
    private Long recordVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BigDecimal usedAmount() {
        return reservedAmount.add(committedAmount);
    }

    public long usedCount() {
        return reservedCount + committedCount;
    }
}
