package com.paymentprocessor.authorization.dto.access;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ScopeValidationRequest(
        List<String> grantedScopes,
        @NotEmpty List<String> requiredScopes
) {
}
