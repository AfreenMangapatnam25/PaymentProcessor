package com.paymentprocessor.authenticationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 200) String password,
        @Size(max = 128) String deviceFingerprint,
        @Size(max = 120) String deviceLabel) {
}
