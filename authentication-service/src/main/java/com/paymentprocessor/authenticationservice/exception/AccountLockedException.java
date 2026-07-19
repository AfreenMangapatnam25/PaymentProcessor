package com.paymentprocessor.authenticationservice.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends ApiException {
    public AccountLockedException(String message) {
        super(HttpStatus.LOCKED, "account_locked", message);
    }
}
