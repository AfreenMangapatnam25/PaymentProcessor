package com.paymentprocessor.authenticationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaDisableRequest(
        @NotBlank String factorId,
        @NotBlank String currentPassword) {
}
