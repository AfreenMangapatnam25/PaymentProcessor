package com.paymentprocessor.authorization.dto.access;

import com.paymentprocessor.authorization.domain.access.Policy;
import com.paymentprocessor.authorization.domain.enums.PolicyEffect;
import com.paymentprocessor.authorization.domain.enums.PolicyType;

import java.util.UUID;

public record PolicyResponse(
        UUID id,
        String name,
        PolicyType type,
        PolicyEffect effect,
        String resource,
        String action,
        String conditionJson,
        int priority,
        boolean enabled
) {
    public static PolicyResponse from(Policy p) {
        return new PolicyResponse(p.getId(), p.getName(), p.getType(), p.getEffect(),
                p.getResource(), p.getAction(), p.getConditionJson(), p.getPriority(), p.isEnabled());
    }
}
