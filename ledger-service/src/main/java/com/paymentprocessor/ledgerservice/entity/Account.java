package com.paymentprocessor.ledgerservice.entity;

import com.paymentprocessor.ledgerservice.domain.enums.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A ledger account in the chart of accounts. May be a General-Ledger control
 * account (identified by {@code accountCode}) or a sub-ledger account tied to a
 * business owner (merchant, customer, platform) via {@code ownerType}/{@code ownerId}.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "account_code", length = 32, nullable = false)
    private String accountCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "owner_type", length = 32)
    private String ownerType;

    @Column(name = "owner_id", length = 64)
    private String ownerId;

    @Column(name = "type_code", length = 16, nullable = false)
    private String typeCode;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "parent_account_id", length = 64)
    private String parentAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getParentAccountId() { return parentAccountId; }
    public void setParentAccountId(String parentAccountId) { this.parentAccountId = parentAccountId; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return status == AccountStatus.ACTIVE; }
}
