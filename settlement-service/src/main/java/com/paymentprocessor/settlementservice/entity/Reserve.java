package com.paymentprocessor.settlementservice.entity;

import com.paymentprocessor.settlementservice.enums.ReserveKind;
import com.paymentprocessor.settlementservice.enums.ReserveStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A rolling / risk reserve amount held back from a merchant settlement and
 * released on a schedule, or consumed to cover chargebacks and reversals.
 */
@Entity
@Table(name = "reserves", indexes = {
        @Index(name = "idx_reserve_merchant", columnList = "merchant_id"),
        @Index(name = "idx_reserve_status", columnList = "status"),
        @Index(name = "idx_reserve_hold_until", columnList = "hold_until")
})
public class Reserve extends BaseEntity {

    @Id
    @Column(name = "id", length = 40)
    private String id;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private ReserveKind kind;

    @Column(name = "rate_bps")
    private Integer rateBps;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "released_minor", nullable = false)
    private long releasedMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "source_batch_id", length = 40)
    private String sourceBatchId;

    @Column(name = "hold_until")
    private LocalDate holdUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReserveStatus status = ReserveStatus.HELD;

    @Column(name = "ledger_hold_id")
    private String ledgerHoldId;

    @Column(name = "released_at")
    private Instant releasedAt;

    /** Amount still held (not yet released or consumed). */
    public long remainingMinor() {
        return amountMinor - releasedMinor;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public ReserveKind getKind() { return kind; }
    public void setKind(ReserveKind kind) { this.kind = kind; }
    public Integer getRateBps() { return rateBps; }
    public void setRateBps(Integer rateBps) { this.rateBps = rateBps; }
    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
    public long getReleasedMinor() { return releasedMinor; }
    public void setReleasedMinor(long releasedMinor) { this.releasedMinor = releasedMinor; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getSourceBatchId() { return sourceBatchId; }
    public void setSourceBatchId(String sourceBatchId) { this.sourceBatchId = sourceBatchId; }
    public LocalDate getHoldUntil() { return holdUntil; }
    public void setHoldUntil(LocalDate holdUntil) { this.holdUntil = holdUntil; }
    public ReserveStatus getStatus() { return status; }
    public void setStatus(ReserveStatus status) { this.status = status; }
    public String getLedgerHoldId() { return ledgerHoldId; }
    public void setLedgerHoldId(String ledgerHoldId) { this.ledgerHoldId = ledgerHoldId; }
    public Instant getReleasedAt() { return releasedAt; }
    public void setReleasedAt(Instant releasedAt) { this.releasedAt = releasedAt; }
}
