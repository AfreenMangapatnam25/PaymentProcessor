package com.paymentprocessor.userservice.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body to register a user. Bean Validation performs the cheap syntactic
 * checks at the boundary; deeper invariants (age, normalization, uniqueness)
 * live in the domain and application layers.
 */
public record CreateUserRequest(

        @NotBlank
        @Size(max = 128)
        String identityId,

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

        String timezone
) {
}
