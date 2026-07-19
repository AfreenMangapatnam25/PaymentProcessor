package com.paymentprocessor.limit.domain.entity;

import com.paymentprocessor.limit.domain.enums.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
 * A time-bound hold placed against one or more usage counters on behalf of a
 * transaction. A single reservation may span several limits (daily, monthly, …);
 * the per-limit holds are recorded in {@link ReservationLine}. The reservation is
 * the unit the Payment Service commits or releases.
 */
@Entity
@Table(
        name = "limit_reservation",
        indexes = {
                @Index(name = "idx_reservation_txn", columnList = "transaction_id"),
                @Index(name = "idx_reservation_status_expiry", columnList = "status,expires_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Payment Service transaction id this reservation belongs to. */
    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    /** Idempotency key so retried reserve calls do not double-hold capacity. */
    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "customer_id", length = 100)
    private String customerId;

    @Column(name = "merchant_id", length = 100)
    private String merchantId;

    @Column(length = 3)
    private String currency;

    @Column(name = "reserved_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal reservedAmount = BigDecimal.ZERO;

    @Column(name = "committed_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal committedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Version
    private Long recordVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isActive() {
        return status == ReservationStatus.RESERVED;
    }
}
