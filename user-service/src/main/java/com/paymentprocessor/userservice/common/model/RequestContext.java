package com.paymentprocessor.userservice.common.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Request context record containing metadata shared across services during request processing.
 * Uses CorrelationId for distributed tracing.
 */
public record RequestContext(
        String requestId,
        CorrelationId correlationId,
        String merchantId,
        String identityId,
        Instant requestTime
) {

    /**
     * Creates a new RequestContext with generated request ID and current timestamp.
     *
     * @param correlationId the correlation ID (can be null, will be generated)
     * @param merchantId    the merchant ID
     * @param identityId    the identity ID
     * @return new RequestContext instance
     */
    public static RequestContext create(CorrelationId correlationId, String merchantId, String identityId) {
        return new RequestContext(
                generateRequestId(),
                correlationId != null ? correlationId : CorrelationId.generate(),
                merchantId,
                identityId,
                Instant.now()
        );
    }

    /**
     * Creates a new RequestContext with all fields provided.
     *
     * @param requestId     the request ID
     * @param correlationId the correlation ID
     * @param merchantId    the merchant ID
     * @param identityId    the identity ID
     * @param requestTime   the request time
     * @return new RequestContext instance
     */
    public static RequestContext of(String requestId, CorrelationId correlationId, String merchantId, String identityId, Instant requestTime) {
        return new RequestContext(requestId, correlationId, merchantId, identityId, requestTime);
    }

    /**
     * Creates a new RequestContext with string correlation ID.
     */
    public static RequestContext of(String requestId, String correlationId, String merchantId, String identityId, Instant requestTime) {
        return new RequestContext(
                requestId,
                CorrelationId.fromString(correlationId),
                merchantId,
                identityId,
                requestTime
        );
    }

    /**
     * Creates a new RequestContext from an existing one, replacing the merchant ID.
     */
    public static RequestContext withMerchantId(RequestContext oldContext, String merchantId) {
        return new RequestContext(
                oldContext.requestId(),
                oldContext.correlationId(),
                merchantId,
                oldContext.identityId(),
                oldContext.requestTime()
        );
    }

    /**
     * Creates a new RequestContext from an existing one, replacing the identity ID.
     */
    public static RequestContext withIdentityId(RequestContext oldContext, String identityId) {
        return new RequestContext(
                oldContext.requestId(),
                oldContext.correlationId(),
                oldContext.merchantId(),
                identityId,
                oldContext.requestTime()
        );
    }

    /**
     * Creates a new RequestContext from an existing one, replacing the correlation ID.
     */
    public static RequestContext withCorrelationId(RequestContext oldContext, CorrelationId correlationId) {
        return new RequestContext(
                oldContext.requestId(),
                correlationId,
                oldContext.merchantId(),
                oldContext.identityId(),
                oldContext.requestTime()
        );
    }

    private static String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Gets the correlation ID as a string.
     *
     * @return the correlation ID string
     */
    public String getCorrelationIdAsString() {
        return correlationId != null ? correlationId.value() : null;
    }

    /**
     * Checks if the context contains all required fields.
     */
    public boolean isValid() {
        return requestId != null && !requestId.isBlank()
                && correlationId != null
                && merchantId != null && !merchantId.isBlank()
                && identityId != null && !identityId.isBlank()
                && requestTime != null;
    }

    /**
     * Gets a map representation of the context for MDC logging.
     */
    public java.util.Map<String, String> toLoggingContext() {
        java.util.Map<String, String> context = new java.util.LinkedHashMap<>();
        context.put("requestId", requestId);
        context.put("correlationId", correlationId != null ? correlationId.value() : null);
        context.put("merchantId", merchantId);
        context.put("identityId", identityId);
        context.put("requestTime", requestTime.toString());
        return context;
    }

    /**
     * Creates a copy of this context with a new request time.
     */
    public RequestContext withRequestTime(Instant newTime) {
        return new RequestContext(requestId, correlationId, merchantId, identityId, newTime);
    }

    /**
     * Creates a copy of this context with a new request ID.
     */
    public RequestContext withRequestId(String newRequestId) {
        return new RequestContext(newRequestId, correlationId, merchantId, identityId, requestTime);
    }
}