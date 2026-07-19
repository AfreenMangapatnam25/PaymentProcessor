package com.paymentprocessor.ledgerservice.entity;

import com.paymentprocessor.ledgerservice.domain.enums.EntryDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A single immutable debit or credit line belonging to a journal. The monotonic
 * {@code id} is used as an append-only high-water mark for balance projection.
 */
@Entity
@Table(name = "entries")
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "journal_id", length = 64, nullable = false)
    private String journalId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "account_id", length = 64, nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 8, nullable = false)
    private EntryDirection direction;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getJournalId() { return journalId; }
    public void setJournalId(String journalId) { this.journalId = journalId; }
    public Integer getLineNumber() { return lineNumber; }
    public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public EntryDirection getDirection() { return direction; }
    public void setDirection(EntryDirection direction) { this.direction = direction; }
    public Long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(Long amountMinor) { this.amountMinor = amountMinor; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(Instant effectiveAt) { this.effectiveAt = effectiveAt; }
}
