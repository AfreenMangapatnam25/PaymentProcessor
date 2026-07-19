package com.paymentprocessor.gatewayservice.audit;

/**
 * Immutable record of a single request as it passed through the gateway. Emitted
 * asynchronously to the audit-service; never on the request's critical path.
 */
public record AuditEvent(
        String correlationId,
        String method,
        String path,
        String routeId,
        String principal,
        String merchantId,
        String clientIp,
        int status,
        long durationMs,
        String timestamp) {
}
