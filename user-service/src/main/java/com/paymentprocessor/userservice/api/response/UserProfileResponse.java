package com.paymentprocessor.userservice.api.response;

import java.time.LocalDate;

/**
 * Profile projection returned to authorized callers. Decrypted at the boundary;
 * null when the user has been erased.
 */
public record UserProfileResponse(
        String email,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String phone,
        String locale,
        String timezone
) {
}
