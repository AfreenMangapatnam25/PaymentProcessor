package com.paymentprocessor.ledgerservice.entity;

import com.paymentprocessor.ledgerservice.domain.enums.SnapshotType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An immutable point-in-time capture of an account's balance for reporting,
 * audit, and reconciliation. Keyed by (accountId, asOfDate).
 */
@Entity
@Table(name = "balance_snapshots")
@IdClass(BalanceSnapshotId.class)
public class BalanceSnapshot {

    @Id
    @Column(name = "account_id", length = 64)
    private String accountId;

    @Id
    @Column(name = "as_of_date")
    private LocalDate asOfDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_type", length = 16, nullable = false)
    private SnapshotType snapshotType = SnapshotType.EOD;

    @Column(name = "opening_minor", nullable = false)
    private Long openingMinor = 0L;

    @Column(name = "debit_minor", nullable = false)
    private Long debitMinor = 0L;

    @Column(name = "credit_minor", nullable = false)
    private Long creditMinor = 0L;

    /** Closing balance, in the account's normal-balance direction. */
    @Column(name = "posted_minor", nullable = false)
    private Long postedMinor = 0L;

    @Column(name = "entry_count", nullable = false)
    private Long entryCount = 0L;

    @Column(name = "entry_high_water", nullable = false)
    private Long entryHighWater = 0L;

    @Column(name = "last_entry_at")
    private Instant lastEntryAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public LocalDate getAsOfDate() { return asOfDate; }
    public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
    public SnapshotType getSnapshotType() { return snapshotType; }
    public void setSnapshotType(SnapshotType snapshotType) { this.snapshotType = snapshotType; }
    public Long getOpeningMinor() { return openingMinor; }
    public void setOpeningMinor(Long openingMinor) { this.openingMinor = openingMinor; }
    public Long getDebitMinor() { return debitMinor; }
    public void setDebitMinor(Long debitMinor) { this.debitMinor = debitMinor; }
    public Long getCreditMinor() { return creditMinor; }
    public void setCreditMinor(Long creditMinor) { this.creditMinor = creditMinor; }
    public Long getPostedMinor() { return postedMinor; }
    public void setPostedMinor(Long postedMinor) { this.postedMinor = postedMinor; }
    public Long getEntryCount() { return entryCount; }
    public void setEntryCount(Long entryCount) { this.entryCount = entryCount; }
    public Long getEntryHighWater() { return entryHighWater; }
    public void setEntryHighWater(Long entryHighWater) { this.entryHighWater = entryHighWater; }
    public Instant getLastEntryAt() { return lastEntryAt; }
    public void setLastEntryAt(Instant lastEntryAt) { this.lastEntryAt = lastEntryAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
