package com.paymentprocessor.authenticationservice.domain;

public enum LoginResult {
    SUCCESS, BAD_CREDENTIALS, MFA_REQUIRED, MFA_FAILED, LOCKED, DISABLED, RATE_LIMITED
}
