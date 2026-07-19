package com.paymentprocessor.settlementservice.entity;

import com.paymentprocessor.settlementservice.enums.BatchStatus;
import com.paymentprocessor.settlementservice.enums.ScheduleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A per-merchant, per-currency aggregation of settlement line items for one
 * settlement period, carrying the calculated monetary breakdown and lifecycle
 * status. Owned resource of the Settlement Service.
 */
@Entity
@Table(name = "settlement_batches", indexes = {
        @Index(name = "idx_batch_merchant", columnList = "merchant_id"),
        @Index(name = "idx_batch_status", columnList = "status")
})
public class SettlementBatch extends BaseEntity {

    @Id
    @Column(name = "id", length = 40)
    private String id;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", length = 20)
    private ScheduleType scheduleType;

    @Column(name = "period_start")
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BatchStatus status = BatchStatus.PENDING;

    // --- monetary breakdown (all minor units, in the batch currency) ---
    @Column(name = "gross_minor", nullable = false)
    private long grossMinor;

    @Column(name = "refunds_minor", nullable = false)
    private long refundsMinor;

    @Column(name = "fees_minor", nullable = false)
    private long feesMinor;

    @Column(name = "interchange_minor", nullable = false)
    private long interchangeMinor;

    @Column(name = "chargebacks_minor", nullable = false)
    private long chargebacksMinor;

    @Column(name = "adjustments_minor", nullable = false)
    private long adjustmentsMinor;

    @Column(name = "reserve_minor", nullable = false)
    private long reserveMinor;

    @Column(name = "settlement_fee_minor", nullable = false)
    private long settlementFeeMinor;

    @Column(name = "net_minor", nullable = false)
    private long netMinor;

    // --- lifecycle metadata ---
    @Column(name = "ledger_journal_id")
    private String ledgerJournalId;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public ScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }
    public Instant getPeriodStart() { return periodStart; }
    public void setPeriodStart(Instant periodStart) { this.periodStart = periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(Instant periodEnd) { this.periodEnd = periodEnd; }
    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }
    public long getGrossMinor() { return grossMinor; }
    public void setGrossMinor(long grossMinor) { this.grossMinor = grossMinor; }
    public long getRefundsMinor() { return refundsMinor; }
    public void setRefundsMinor(long refundsMinor) { this.refundsMinor = refundsMinor; }
    public long getFeesMinor() { return feesMinor; }
    public void setFeesMinor(long feesMinor) { this.feesMinor = feesMinor; }
    public long getInterchangeMinor() { return interchangeMinor; }
    public void setInterchangeMinor(long interchangeMinor) { this.interchangeMinor = interchangeMinor; }
    public long getChargebacksMinor() { return chargebacksMinor; }
    public void setChargebacksMinor(long chargebacksMinor) { this.chargebacksMinor = chargebacksMinor; }
    public long getAdjustmentsMinor() { return adjustmentsMinor; }
    public void setAdjustmentsMinor(long adjustmentsMinor) { this.adjustmentsMinor = adjustmentsMinor; }
    public long getReserveMinor() { return reserveMinor; }
    public void setReserveMinor(long reserveMinor) { this.reserveMinor = reserveMinor; }
    public long getSettlementFeeMinor() { return settlementFeeMinor; }
    public void setSettlementFeeMinor(long settlementFeeMinor) { this.settlementFeeMinor = settlementFeeMinor; }
    public long getNetMinor() { return netMinor; }
    public void setNetMinor(long netMinor) { this.netMinor = netMinor; }
    public String getLedgerJournalId() { return ledgerJournalId; }
    public void setLedgerJournalId(String ledgerJournalId) { this.ledgerJournalId = ledgerJournalId; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
