package com.paymentprocessor.userservice.application.command;

import java.time.LocalDate;

/**
 * Command to replace a user's profile. {@code expectedVersion} enforces
 * optimistic locking against concurrent edits.
 */
public record UpdateUserProfileCommand(
        String userId,
        String email,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String phone,
        String locale,
        String timezone,
        long expectedVersion
) {
}
