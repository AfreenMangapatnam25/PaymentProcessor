package com.paymentprocessor.authenticationservice.dto;

import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterIdentityRequest(
        @NotNull PrincipalType principalType,
        @NotBlank @Email @Size(max = 320) String email,
        @Size(max = 20) String phoneE164,
        @NotBlank @Size(min = 12, max = 200) String password,
        boolean mfaRequired) {
}
