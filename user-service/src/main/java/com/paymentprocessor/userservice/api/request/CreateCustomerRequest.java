package com.paymentprocessor.userservice.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request to create a customer. The merchant is taken from the caller's token,
 * never from this body.
 */
public record CreateCustomerRequest(

        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(max = 200)
        String fullName,

        String phone,

        @Size(max = 128)
        String externalRef,

        String userId,

        Map<String, String> metadata
) {
}
