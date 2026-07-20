package com.paymentprocessor.authorization.dto.access;

import com.paymentprocessor.authorization.domain.enums.AccessDecision;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * Result of an access decision.
 */
@Builder
public record AccessDecisionResponse(
        AccessDecision decision,
        String reasonCode,
        String reason,
        String identityId,
        String resource,
        String action,
        List<String> evaluatedPolicies,
        Instant evaluatedAt
) {
    public boolean allowed() {
        return decision == AccessDecision.ALLOW;
    }
}
