package com.paymentprocessor.settlementservice.entity;

import com.paymentprocessor.settlementservice.enums.SettlementItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A single financial line item that contributes to a settlement batch: a
 * capture, refund, chargeback, fee, reserve movement, or adjustment. The
 * {@code amountMinor} is always stored as a positive magnitude; the direction
 * is derived from {@link SettlementItemType#getSign()}.
 */
@Entity
@Table(name = "settlement_items", indexes = {
        @Index(name = "idx_item_batch", columnList = "batch_id"),
        @Index(name = "idx_item_merchant", columnList = "merchant_id"),
        @Index(name = "idx_item_source", columnList = "source_type,source_id")
})
public class SettlementItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null until the item is assigned to a batch during aggregation. */
    @Column(name = "batch_id", length = 40)
    private String batchId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private SettlementItemType type;

    /** Upstream source category (e.g. PAYMENT, REFUND, CHARGEBACK, ADJUSTMENT). */
    @Column(name = "source_type", length = 30)
    private String sourceType;

    /** Upstream source identifier (e.g. the payment/refund id). */
    @Column(name = "source_id")
    private String sourceId;

    /** Positive magnitude in minor units; sign comes from {@link #type}. */
    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    /** Guards against ingesting the same upstream event twice. */
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    /** The signed contribution of this item to the net settlement. */
    public long signedAmountMinor() {
        return amountMinor * type.getSign();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public SettlementItemType getType() { return type; }
    public void setType(SettlementItemType type) { this.type = type; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(Instant effectiveAt) { this.effectiveAt = effectiveAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
