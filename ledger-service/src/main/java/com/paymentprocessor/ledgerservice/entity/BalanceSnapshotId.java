package com.paymentprocessor.ledgerservice.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class BalanceSnapshotId implements Serializable {

    private String accountId;
    private LocalDate asOfDate;

    public BalanceSnapshotId() { }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public LocalDate getAsOfDate() { return asOfDate; }
    public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BalanceSnapshotId that = (BalanceSnapshotId) o;
        return Objects.equals(accountId, that.accountId) && Objects.equals(asOfDate, that.asOfDate);
    }

    @Override
    public int hashCode() { return Objects.hash(accountId, asOfDate); }
}
