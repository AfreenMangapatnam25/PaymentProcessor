package com.paymentprocessor.settlementservice.entity;

import com.paymentprocessor.settlementservice.enums.FailureCategory;
import com.paymentprocessor.settlementservice.enums.PayoutStatus;
import com.paymentprocessor.settlementservice.enums.Rail;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The instruction to move a net settlement amount to a merchant's bank account
 * over a specific rail, together with its execution status and retry state.
 */
@Entity
@Table(name = "payouts", indexes = {
        @Index(name = "idx_payout_batch", columnList = "batch_id"),
        @Index(name = "idx_payout_merchant", columnList = "merchant_id"),
        @Index(name = "idx_payout_status", columnList = "status"),
        @Index(name = "idx_payout_next_retry", columnList = "next_retry_at")
})
public class Payout extends BaseEntity {

    @Id
    @Column(name = "id", length = 40)
    private String id;

    @Column(name = "batch_id", nullable = false, length = 40)
    private String batchId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "payout_account_id")
    private String payoutAccountId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "rail", length = 20)
    private Rail rail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(name = "provider_ref")
    private String providerRef;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "ledger_journal_id")
    private String ledgerJournalId;

    // --- retry / failure tracking ---
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "failure_code")
    private String failureCode;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_category", length = 20)
    private FailureCategory failureCategory;

    // --- timestamps ---
    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getPayoutAccountId() { return payoutAccountId; }
    public void setPayoutAccountId(String payoutAccountId) { this.payoutAccountId = payoutAccountId; }
    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Rail getRail() { return rail; }
    public void setRail(Rail rail) { this.rail = rail; }
    public PayoutStatus getStatus() { return status; }
    public void setStatus(PayoutStatus status) { this.status = status; }
    public String getProviderRef() { return providerRef; }
    public void setProviderRef(String providerRef) { this.providerRef = providerRef; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getLedgerJournalId() { return ledgerJournalId; }
    public void setLedgerJournalId(String ledgerJournalId) { this.ledgerJournalId = ledgerJournalId; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public FailureCategory getFailureCategory() { return failureCategory; }
    public void setFailureCategory(FailureCategory failureCategory) { this.failureCategory = failureCategory; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
