package com.paymentprocessor.reconciliationservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published for each discrepancy that exceeds auto-match tolerance or fails validation. Consumed by
 * Operations, Notification, Fraud and Audit services.
 */
public record MismatchDetectedEvent(
        UUID exceptionUuid,
        UUID runUuid,
        String reconType,
        String channel,
        String category,
        String severityLevel,
        BigDecimal severityScore,
        BigDecimal amount,
        String currency,
        String externalReference,
        String reviewQueue,
        String description,
        Instant detectedAt
) {
}
