package com.paymentprocessor.userservice.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request to update a customer's contact details and metadata.
 */
public record UpdateCustomerRequest(

        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(max = 200)
        String fullName,

        String phone,

        Map<String, String> metadata,

        @NotNull
        @PositiveOrZero
        Long expectedVersion
) {
}
