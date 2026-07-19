package com.paymentprocessor.authenticationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmVerificationRequest(
        @NotBlank String token,
        String code) {
}
