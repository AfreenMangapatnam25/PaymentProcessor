package com.paymentprocessor.limit.domain.entity;

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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The hold placed by one reservation against one usage counter. Keeping a line per
 * (reservation, counter) lets release and partial-capture restore the exact amount
 * and count that were held against each individual limit.
 */
@Entity
@Table(
        name = "reservation_line",
        indexes = @Index(name = "idx_res_line_reservation", columnList = "reservation_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "limit_config_id", nullable = false)
    private UUID limitConfigId;

    @Column(name = "usage_counter_id", nullable = false)
    private UUID usageCounterId;

    @Column(name = "window_key", nullable = false, length = 40)
    private String windowKey;

    @Column(name = "held_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal heldAmount = BigDecimal.ZERO;

    @Column(name = "held_count", nullable = false)
    @Builder.Default
    private long heldCount = 0L;

    /** Portion of {@link #heldAmount} already converted to committed usage. */
    @Column(name = "committed_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal committedAmount = BigDecimal.ZERO;
}
