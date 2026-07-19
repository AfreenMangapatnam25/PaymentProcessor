package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.KybCaseStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Callback from the Compliance/KYB Service recording a verification decision. */
public record KybDecisionRequest(
        @NotNull KybCaseStatus decision,
        Integer riskScore,
        boolean sanctionsScreened,
        boolean pepScreened,
        @Size(max = 1024) String decisionReason,
        @Size(max = 100) String externalReference
) {}
