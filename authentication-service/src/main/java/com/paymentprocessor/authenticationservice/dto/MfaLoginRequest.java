package com.paymentprocessor.authenticationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaLoginRequest(
        @NotBlank String mfaToken,
        @NotBlank String code,
        String deviceFingerprint,
        String deviceLabel) {
}
