package com.paymentprocessor.userservice.api.response;

import com.paymentprocessor.userservice.common.model.CorrelationId;
import com.paymentprocessor.userservice.common.model.RequestContext;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Metadata record for API responses.
 * Contains timestamp, request ID, correlation ID, and optional status/message.
 */
public record Meta(
        Instant timestamp,
        String requestId,
        String correlationId,
        Integer status,
        String message
) {

    /**
     * Creates Meta with only timestamp.
     */
    public static Meta of(Instant timestamp) {
        return new Meta(timestamp, null, null, null, null);
    }

    /**
     * Creates Meta with timestamp and request context.
     */
    public static Meta fromContext(RequestContext context) {
        if (context == null) {
            return of(Instant.now());
        }
        return new Meta(
                context.requestTime(),
                context.requestId(),
                context.getCorrelationIdAsString(),
                null,
                null
        );
    }

    /**
     * Creates Meta with timestamp, request context, and status.
     */
    public static Meta fromContext(RequestContext context, HttpStatus status) {
        if (context == null) {
            return new Meta(Instant.now(), null, null, status != null ? status.value() : null, null);
        }
        return new Meta(
                context.requestTime(),
                context.requestId(),
                context.getCorrelationIdAsString(),
                status != null ? status.value() : null,
                null
        );
    }

    /**
     * Creates Meta with timestamp, request context, status, and message.
     */
    public static Meta fromContext(RequestContext context, HttpStatus status, String message) {
        if (context == null) {
            return new Meta(Instant.now(), null, null, status != null ? status.value() : null, message);
        }
        return new Meta(
                context.requestTime(),
                context.requestId(),
                context.getCorrelationIdAsString(),
                status != null ? status.value() : null,
                message
        );
    }

    /**
     * Creates Meta from a CorrelationId.
     */
    public static Meta fromCorrelationId(CorrelationId correlationId) {
        return new Meta(
                Instant.now(),
                null,
                correlationId != null ? correlationId.value() : null,
                null,
                null
        );
    }

    /**
     * Creates Meta for an error response.
     */
    public static Meta error(HttpStatus status, String message) {
        return new Meta(Instant.now(), null, null, status.value(), message);
    }

    /**
     * Creates Meta for an error response with request context.
     */
    public static Meta error(RequestContext context, HttpStatus status, String message) {
        if (context == null) {
            return error(status, message);
        }
        return new Meta(
                context.requestTime(),
                context.requestId(),
                context.getCorrelationIdAsString(),
                status.value(),
                message
        );
    }

    /**
     * Adds a message to the metadata.
     */
    public Meta withMessage(String message) {
        return new Meta(timestamp, requestId, correlationId, status, message);
    }

    /**
     * Adds a status to the metadata.
     */
    public Meta withStatus(HttpStatus status) {
        return new Meta(timestamp, requestId, correlationId, status != null ? status.value() : null, message);
    }

    /**
     * Adds a request ID to the metadata.
     */
    public Meta withRequestId(String requestId) {
        return new Meta(timestamp, requestId, correlationId, status, message);
    }

    /**
     * Adds a correlation ID to the metadata.
     */
    public Meta withCorrelationId(CorrelationId correlationId) {
        return new Meta(
                timestamp,
                requestId,
                correlationId != null ? correlationId.value() : null,
                status,
                message
        );
    }

    /**
     * Adds a correlation ID string to the metadata.
     */
    public Meta withCorrelationId(String correlationId) {
        return new Meta(timestamp, requestId, correlationId, status, message);
    }

    /**
     * Checks if this metadata represents a successful response.
     */
    public boolean isSuccess() {
        return status == null || (status >= 200 && status < 300);
    }

    /**
     * Checks if this metadata represents an error response.
     */
    public boolean isError() {
        return status != null && (status >= 400 || status < 200);
    }

    /**
     * Gets the HTTP status as an enum.
     */
    public HttpStatus getHttpStatus() {
        return status != null ? HttpStatus.valueOf(status) : null;
    }
}