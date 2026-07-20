package com.paymentprocessor.settlementservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Records funds returned by a receiving bank after a payout was submitted
 * (e.g. invalid account, account closed). Drives a payout into the RETURNED
 * state and a compensating ledger entry.
 */
@Entity
@Table(name = "payout_returns", indexes = {
        @Index(name = "idx_return_payout", columnList = "payout_id")
})
public class PayoutReturn extends BaseEntity {

    @Id
    @Column(name = "id", length = 40)
    private String id;

    @Column(name = "payout_id", nullable = false, length = 40)
    private String payoutId;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "reason_description", length = 512)
    private String reasonDescription;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "returned_at", nullable = false)
    private Instant returnedAt;

    @Column(name = "ledger_journal_id")
    private String ledgerJournalId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPayoutId() { return payoutId; }
    public void setPayoutId(String payoutId) { this.payoutId = payoutId; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getReasonDescription() { return reasonDescription; }
    public void setReasonDescription(String reasonDescription) { this.reasonDescription = reasonDescription; }
    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getReturnedAt() { return returnedAt; }
    public void setReturnedAt(Instant returnedAt) { this.returnedAt = returnedAt; }
    public String getLedgerJournalId() { return ledgerJournalId; }
    public void setLedgerJournalId(String ledgerJournalId) { this.ledgerJournalId = ledgerJournalId; }
}
