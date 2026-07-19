package com.paymentprocessor.authenticationservice.dto;

import com.paymentprocessor.authenticationservice.domain.PrincipalType;

import java.util.Set;

public record ApiKeyVerifyResponse(
        boolean valid,
        String keyId,
        PrincipalType ownerType,
        String ownerId,
        Set<String> scopes,
        String environment) {

    public static ApiKeyVerifyResponse invalid() {
        return new ApiKeyVerifyResponse(false, null, null, null, null, null);
    }
}
