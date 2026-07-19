package com.paymentprocessor.authenticationservice.dto;

import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;

public record ApiKeyCreateRequest(
        @NotNull PrincipalType ownerType,
        @NotBlank String ownerId,
        Set<String> scopes,
        @NotBlank String environment,
        Instant expiresAt) {
}
