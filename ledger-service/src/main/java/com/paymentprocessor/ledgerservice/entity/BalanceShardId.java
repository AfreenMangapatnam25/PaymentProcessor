package com.paymentprocessor.ledgerservice.entity;

import java.io.Serializable;
import java.util.Objects;

public class BalanceShardId implements Serializable {

    private String accountId;
    private Short shard;

    public BalanceShardId() { }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public Short getShard() { return shard; }
    public void setShard(Short shard) { this.shard = shard; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BalanceShardId that = (BalanceShardId) o;
        return Objects.equals(accountId, that.accountId) && Objects.equals(shard, that.shard);
    }

    @Override
    public int hashCode() { return Objects.hash(accountId, shard); }
}
