package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.KybCaseStatus;
import java.time.Instant;
import java.util.UUID;

public record KybCaseResponse(
        UUID id, UUID merchantId, KybCaseStatus status, String externalReference,
        Integer riskScore, boolean sanctionsScreened, boolean pepScreened,
        String decisionReason, Instant submittedAt, Instant reviewedAt
) {}
