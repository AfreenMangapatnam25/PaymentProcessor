package com.paymentprocessor.userservice.domain.user;

public enum UserStatus {
    ACTIVE,

    SUSPENDED,

    LOCKED,

    ERASED;

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean canLogin() {
        return this == ACTIVE;
    }

    public boolean isTerminalState() {
        return this == ERASED;
    }
}
