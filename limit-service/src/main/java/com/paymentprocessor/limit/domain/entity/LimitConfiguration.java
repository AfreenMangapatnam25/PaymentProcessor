package com.paymentprocessor.limit.domain.entity;

import com.paymentprocessor.limit.domain.enums.EnforcementMode;
import com.paymentprocessor.limit.domain.enums.EntityScope;
import com.paymentprocessor.limit.domain.enums.LimitDimension;
import com.paymentprocessor.limit.domain.enums.TimeWindow;
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
 * A single limit rule. A configuration is resolved for a transaction when its
 * scope + scopeId match the request (or scopeId is null, meaning a default that
 * applies to the whole scope) and its currency matches (or is null, meaning any).
 *
 * <p>Each configuration constrains exactly one {@link LimitDimension} over one
 * {@link TimeWindow}. A customer daily amount cap and a customer daily count cap
 * are therefore two separate rows.
 */
@Entity
@Table(
        name = "limit_configuration",
        indexes = {
                @Index(name = "idx_limit_cfg_lookup", columnList = "scope,scope_id,active"),
                @Index(name = "idx_limit_cfg_currency", columnList = "currency")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Human-readable name, e.g. "Customer default daily amount". */
    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntityScope scope;

    /**
     * Identifier of the entity this limit applies to (customer id, merchant id,
     * ISO country code, currency code). Null means a scope-wide default that
     * applies to every entity in the scope.
     */
    @Column(name = "scope_id", length = 100)
    private String scopeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LimitDimension dimension;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_window", nullable = false, length = 20)
    private TimeWindow timeWindow;

    /**
     * Threshold value. For AMOUNT dimensions this is a monetary value in
     * {@link #currency}; for COUNT dimensions it is a whole number of transactions.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal threshold;

    /** ISO-4217 currency code for AMOUNT limits. Null = applies to any currency. */
    @Column(length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EnforcementMode enforcement;

    /**
     * Precedence weight. When multiple limits apply, the most restrictive still
     * governs the decision; priority is used for deterministic ordering and to
     * mirror the README hierarchy (higher = evaluated/reported first).
     */
    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean active;

    /** IANA time-zone id used to compute window boundaries (e.g. "Asia/Kolkata"). */
    @Column(name = "time_zone", nullable = false, length = 60)
    @Builder.Default
    private String timeZone = "UTC";

    @Version
    private Long recordVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isAmount() {
        return dimension == LimitDimension.AMOUNT;
    }
}
