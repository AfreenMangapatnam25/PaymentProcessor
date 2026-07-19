package com.paymentprocessor.authenticationservice.dto;

import java.util.List;

public record IntrospectResponse(
        boolean active,
        String identityId,
        String principalType,
        List<String> scopes,
        Long expiresAt) {

    public static IntrospectResponse inactive() {
        return new IntrospectResponse(false, null, null, null, null);
    }
}
