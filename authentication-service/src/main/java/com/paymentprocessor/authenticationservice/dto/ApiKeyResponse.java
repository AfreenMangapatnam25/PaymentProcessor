package com.paymentprocessor.authenticationservice.dto;

import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import com.paymentprocessor.authenticationservice.entity.ApiKey;

import java.time.Instant;
import java.util.Set;

public record ApiKeyResponse(
        String id,
        PrincipalType ownerType,
        String ownerId,
        String prefix,
        Set<String> scopes,
        String environment,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt) {

    public static ApiKeyResponse from(ApiKey k) {
        return new ApiKeyResponse(k.getId(), k.getOwnerType(), k.getOwnerId(), k.getPrefix(),
                k.getScopes(), k.getEnvironment(), k.getExpiresAt(), k.getRevokedAt(), k.getCreatedAt());
    }
}
