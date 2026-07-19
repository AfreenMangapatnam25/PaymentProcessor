package com.paymentprocessor.userservice.domain.customer;

/**
 * Lifecycle of a merchant-scoped customer. DELETED is a soft delete (row
 * retained); ERASED is the terminal crypto-shredded state.
 */
public enum CustomerStatus {
    ACTIVE,
    INACTIVE,
    DELETED,
    ERASED;

    public boolean isTerminal() {
        return this == ERASED;
    }

    public boolean isDeleted() {
        return this == DELETED || this == ERASED;
    }
}
