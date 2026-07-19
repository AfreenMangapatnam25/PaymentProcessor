package com.paymentprocessor.authorization.dto.access;

import com.paymentprocessor.authorization.domain.access.RoleAssignment;
import com.paymentprocessor.authorization.domain.enums.RoleScope;

import java.time.Instant;
import java.util.UUID;

public record RoleAssignmentResponse(
        UUID id,
        String identityId,
        String roleName,
        RoleScope scope,
        String scopeId,
        Instant validFrom,
        Instant validUntil
) {
    public static RoleAssignmentResponse from(RoleAssignment a) {
        return new RoleAssignmentResponse(
                a.getId(), a.getIdentityId(), a.getRole().getName(),
                a.getScope(), a.getScopeId(), a.getValidFrom(), a.getValidUntil());
    }
}
