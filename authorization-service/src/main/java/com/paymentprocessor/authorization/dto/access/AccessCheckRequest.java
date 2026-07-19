package com.paymentprocessor.authorization.dto.access;

import com.paymentprocessor.authorization.domain.enums.RoleScope;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * Request for an access decision. {@code identityId} may be omitted to use the caller resolved from
 * the bearer token. {@code attributes} supply subject/resource/environment values for ABAC,
 * flattened as e.g. {@code subject.kyc_status}, {@code resource.amount},
 * {@code environment.device_trust_level}.
 */
public record AccessCheckRequest(
        String identityId,
        @NotBlank String resource,
        @NotBlank String action,
        RoleScope scope,
        String scopeId,
        String resourceOwnerId,
        List<String> requiredScopes,
        Map<String, Object> attributes
) {
}
