package com.paymentprocessor.disputeservice.entity;

import com.paymentprocessor.disputeservice.domain.enums.LiabilityParty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * The financial impact record for a dispute: the amounts debited from the
 * merchant on chargeback receipt, the reserve tier applied, and the ledger
 * postings that back them. Marked reversed when a dispute is won and the funds
 * are returned.
 */
@Entity
@Table(name = "liability", indexes = {
        @Index(name = "idx_liability_dispute", columnList = "dispute_id")
})
public class Liability {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "dispute_id", nullable = false)
    private String disputeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "party", length = 20)
    private LiabilityParty party = LiabilityParty.PENDING;

    @Column(name = "disputed_amount_minor")
    private Long disputedAmountMinor;

    @Column(name = "fee_minor")
    private Long feeMinor;

    @Column(name = "total_minor")
    private Long totalMinor;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "reserve_tier", length = 20)
    private String reserveTier;

    @Column(name = "reserve_percentage")
    private Integer reservePercentage;

    @Column(name = "ledger_hold_id")
    private String ledgerHoldId;

    @Column(name = "ledger_journal_id")
    private String ledgerJournalId;

    @Column(name = "reversed")
    private boolean reversed;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = "lia_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisputeId() { return disputeId; }
    public void setDisputeId(String disputeId) { this.disputeId = disputeId; }
    public LiabilityParty getParty() { return party; }
    public void setParty(LiabilityParty party) { this.party = party; }
    public Long getDisputedAmountMinor() { return disputedAmountMinor; }
    public void setDisputedAmountMinor(Long disputedAmountMinor) { this.disputedAmountMinor = disputedAmountMinor; }
    public Long getFeeMinor() { return feeMinor; }
    public void setFeeMinor(Long feeMinor) { this.feeMinor = feeMinor; }
    public Long getTotalMinor() { return totalMinor; }
    public void setTotalMinor(Long totalMinor) { this.totalMinor = totalMinor; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getReserveTier() { return reserveTier; }
    public void setReserveTier(String reserveTier) { this.reserveTier = reserveTier; }
    public Integer getReservePercentage() { return reservePercentage; }
    public void setReservePercentage(Integer reservePercentage) { this.reservePercentage = reservePercentage; }
    public String getLedgerHoldId() { return ledgerHoldId; }
    public void setLedgerHoldId(String ledgerHoldId) { this.ledgerHoldId = ledgerHoldId; }
    public String getLedgerJournalId() { return ledgerJournalId; }
    public void setLedgerJournalId(String ledgerJournalId) { this.ledgerJournalId = ledgerJournalId; }
    public boolean isReversed() { return reversed; }
    public void setReversed(boolean reversed) { this.reversed = reversed; }
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
    public Instant getReversedAt() { return reversedAt; }
    public void setReversedAt(Instant reversedAt) { this.reversedAt = reversedAt; }
}
