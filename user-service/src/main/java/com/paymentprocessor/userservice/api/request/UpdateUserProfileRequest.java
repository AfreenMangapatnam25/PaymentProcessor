package com.paymentprocessor.userservice.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to replace a user's profile. Full-representation PUT semantics.
 */
public record UpdateUserProfileRequest(

        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotNull
        @Past
        LocalDate dateOfBirth,

        String phone,

        String locale,

        String timezone,

        @NotNull
        @PositiveOrZero
        Long expectedVersion
) {
}
