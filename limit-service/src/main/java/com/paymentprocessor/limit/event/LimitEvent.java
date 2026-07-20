package com.paymentprocessor.limit.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Domain events published to Kafka for downstream consumers (Fraud, Notification,
 * Audit, Compliance). See README "Domain Events".
 */
public final class LimitEvent {

    private LimitEvent() {}

    public record LimitReserved(
            String eventId,
            String reservationId,
            String transactionId,
            String customerId,
            String merchantId,
            String currency,
            BigDecimal reservedAmount,
            Instant expiresAt,
            Instant occurredAt) {}

    public record LimitReleased(
            String eventId,
            String reservationId,
            String transactionId,
            String reason,
            BigDecimal releasedAmount,
            Instant occurredAt) {}

    public record Violation(
            String limitConfigId,
            String limitName,
            String dimension,
            String timeWindow,
            String enforcement,
            BigDecimal threshold,
            BigDecimal attempted) {}

    public record LimitExceeded(
            String eventId,
            String transactionId,
            String customerId,
            String merchantId,
            String currency,
            String decision,
            List<Violation> violations,
            Instant occurredAt) {}
}
