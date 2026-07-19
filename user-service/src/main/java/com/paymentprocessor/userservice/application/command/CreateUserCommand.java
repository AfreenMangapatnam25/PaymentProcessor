package com.paymentprocessor.userservice.application.command;

import java.time.LocalDate;

/**
 * Application command to register a new user. Carries primitives only; the
 * command service turns them into validated domain value objects. Immutable
 * (rule 6).
 */
public record CreateUserCommand(
        String identityId,
        String email,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String phone,
        String locale,
        String timezone
) {
}
