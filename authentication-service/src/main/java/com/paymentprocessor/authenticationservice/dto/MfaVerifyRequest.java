package com.paymentprocessor.authenticationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(
        @NotBlank String factorId,
        @NotBlank String code) {
}
