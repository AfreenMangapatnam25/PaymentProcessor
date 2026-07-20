package com.paymentprocessor.userservice.application.command;

/**
 * Command to transition a user's lifecycle status. {@code expectedVersion}
 * carries the optimistic-lock token the client last read (rule 8).
 */
public record UpdateUserStatusCommand(
        String userId,
        UserStatusAction action,
        long expectedVersion
) {
}
