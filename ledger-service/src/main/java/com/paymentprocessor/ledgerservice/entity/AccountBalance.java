package com.paymentprocessor.ledgerservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * Denormalised, real-time balance for an account, protected by optimistic
 * locking ({@link Version}) so concurrent postings cannot cause drift.
 *
 * <p>{@code postedMinor} is expressed in the account's <em>normal-balance</em>
 * direction: a positive value means a balance on the account's normal side.
 * {@code availableMinor = postedMinor - heldMinor}.</p>
 */
@Entity
@Table(name = "account_balances")
public class AccountBalance {

    @Id
    @Column(name = "account_id", length = 64)
    private String accountId;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "posted_minor", nullable = false)
    private Long postedMinor = 0L;

    @Column(name = "pending_minor", nullable = false)
    private Long pendingMinor = 0L;

    @Column(name = "held_minor", nullable = false)
    private Long heldMinor = 0L;

    @Column(name = "available_minor", nullable = false)
    private Long availableMinor = 0L;

    @Column(name = "entry_high_water", nullable = false)
    private Long entryHighWater = 0L;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Recompute the available balance from posted and held amounts. */
    public void recomputeAvailable() {
        long posted = postedMinor == null ? 0L : postedMinor;
        long held = heldMinor == null ? 0L : heldMinor;
        this.availableMinor = posted - held;
    }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Long getPostedMinor() { return postedMinor; }
    public void setPostedMinor(Long postedMinor) { this.postedMinor = postedMinor; }
    public Long getPendingMinor() { return pendingMinor; }
    public void setPendingMinor(Long pendingMinor) { this.pendingMinor = pendingMinor; }
    public Long getHeldMinor() { return heldMinor; }
    public void setHeldMinor(Long heldMinor) { this.heldMinor = heldMinor; }
    public Long getAvailableMinor() { return availableMinor; }
    public void setAvailableMinor(Long availableMinor) { this.availableMinor = availableMinor; }
    public Long getEntryHighWater() { return entryHighWater; }
    public void setEntryHighWater(Long entryHighWater) { this.entryHighWater = entryHighWater; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
