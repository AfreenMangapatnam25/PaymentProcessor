package com.paymentprocessor.userservice.api.response;

import com.paymentprocessor.userservice.common.model.RequestContext;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Generic API response wrapper for all REST endpoints.
 * Provides a consistent response structure with metadata.
 *
 * @param <T> the type of the data payload
 */
public record ApiResponse<T>(
        T data,
        Meta meta
) {

    /**
     * Creates a successful API response with data.
     *
     * @param data the response data
     * @param meta the metadata
     * @param <T>  the type of data
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> success(T data, Meta meta) {
        return new ApiResponse<>(data, meta);
    }

    /**
     * Creates a successful API response with data and current timestamp.
     *
     * @param data the response data
     * @param <T>  the type of data
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, Meta.of(Instant.now()));
    }

    /**
     * Creates a successful API response with data and request context.
     *
     * @param data    the response data
     * @param context the request context
     * @param <T>     the type of data
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> success(T data, RequestContext context) {
        return new ApiResponse<>(data, Meta.fromContext(context));
    }

    /**
     * Creates a successful API response with data, request context, and status.
     *
     * @param data    the response data
     * @param context the request context
     * @param status  the HTTP status
     * @param <T>     the type of data
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> success(T data, RequestContext context, HttpStatus status) {
        return new ApiResponse<>(data, Meta.fromContext(context, status));
    }

    /**
     * Creates an error API response with no data.
     *
     * @param meta the error metadata
     * @param <T>  the type of data
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> error(Meta meta) {
        return new ApiResponse<>(null, meta);
    }

    /**
     * Creates an error API response with metadata from context.
     *
     * @param context the request context
     * @param status  the HTTP status
     * @param <T>     the type of data
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> error(RequestContext context, HttpStatus status) {
        return new ApiResponse<>(null, Meta.fromContext(context, status));
    }

    /**
     * Creates an error API response with custom message.
     *
     * @param context the request context
     * @param status  the HTTP status
     * @param message the error message
     * @param <T>     the type of data
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> error(RequestContext context, HttpStatus status, String message) {
        return new ApiResponse<>(null, Meta.fromContext(context, status, message));
    }

    /**
     * Checks if the response is successful (has data).
     *
     * @return true if data is present, false otherwise
     */
    public boolean isSuccess() {
        return data != null;
    }

    /**
     * Checks if the response is an error (no data).
     *
     * @return true if data is null, false otherwise
     */
    public boolean isError() {
        return data == null;
    }

    /**
     * Gets the correlation ID from metadata.
     *
     * @return the correlation ID or null
     */
    public String getCorrelationId() {
        return meta != null ? meta.correlationId() : null;
    }

    /**
     * Gets the request ID from metadata.
     *
     * @return the request ID or null
     */
    public String getRequestId() {
        return meta != null ? meta.requestId() : null;
    }

    /**
     * Maps the data to a different type.
     *
     * @param mapper the mapping function
     * @param <U>    the new data type
     * @return ApiResponse with mapped data
     */
    public <U> ApiResponse<U> map(java.util.function.Function<T, U> mapper) {
        if (data == null) {
            return new ApiResponse<>(null, meta);
        }
        return new ApiResponse<>(mapper.apply(data), meta);
    }
}
