package com.paymentprocessor.userservice.application.command;

/**
 * The status transitions a client may request. Mapping an action to a domain
 * operation keeps controllers free of business logic (rule 2).
 */
public enum UserStatusAction {
    ACTIVATE,
    SUSPEND,
    LOCK
}
