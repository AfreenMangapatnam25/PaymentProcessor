package com.paymentprocessor.userservice.application.command;

/**
 * Command to GDPR-erase a user (crypto-shred). {@code expectedVersion} guards
 * against erasing a version other than the one the operator reviewed.
 */
public record EraseUserCommand(
        String userId,
        long expectedVersion
) {
}
