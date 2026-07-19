package com.paymentprocessor.ledgerservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "balance_shards")
@IdClass(BalanceShardId.class)
public class BalanceShard {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @Id
    @Column(name = "shard")
    private Short shard;

    @Column(name = "posted_minor")
    private Long postedMinor;

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public Short getShard() { return shard; }
    public void setShard(Short shard) { this.shard = shard; }
    public Long getPostedMinor() { return postedMinor; }
    public void setPostedMinor(Long postedMinor) { this.postedMinor = postedMinor; }
}
