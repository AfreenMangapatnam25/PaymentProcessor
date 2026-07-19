package com.paymentprocessor.authorization.dto.access;

import com.paymentprocessor.authorization.domain.enums.PolicyEffect;
import com.paymentprocessor.authorization.domain.enums.PolicyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PolicyRequest(
        @NotBlank String name,
        @NotNull PolicyType type,
        @NotNull PolicyEffect effect,
        @NotBlank String resource,
        @NotBlank String action,
        String conditionJson,
        int priority,
        boolean enabled
) {
}
