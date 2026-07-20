package com.paymentprocessor.authorization.dto.access;

import com.paymentprocessor.authorization.domain.enums.RoleScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RoleAssignmentRequest(
        @NotBlank String identityId,
        @NotBlank String roleName,
        @NotNull RoleScope scope,
        String scopeId,
        Instant validFrom,
        Instant validUntil
) {
}
