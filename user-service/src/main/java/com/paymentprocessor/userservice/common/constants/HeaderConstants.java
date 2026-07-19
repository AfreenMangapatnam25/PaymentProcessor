package com.paymentprocessor.userservice.common.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Constants for HTTP header names used across the application.
 * All header names are in standard hyphenated format (kebab-case) as per HTTP conventions,
 * except for AUTHORIZATION which follows the standard HTTP Authorization header format.
 *
 * @author Development Team
 * @version 1.0
 */
public final class HeaderConstants {

    /**
     * Private constructor to prevent instantiation.
     * This class is designed to be used only for its static constants.
     */
    private HeaderConstants() {
        throw new UnsupportedOperationException("HeaderConstants is a utility class and cannot be instantiated");
    }

    /**
     * Request ID header used for tracking individual HTTP requests.
     * Used for logging and debugging purposes to correlate logs for a specific request.
     */
    public static final String X_REQUEST_ID = "X-Request-ID";

    /**
     * Correlation ID header used for tracing requests across multiple services.
     * Allows tracking of a transaction flow through distributed systems.
     */
    public static final String X_CORRELATION_ID = "X-Correlation-ID";

    /**
     * Merchant ID header identifying the merchant making the request.
     * Used in multi-tenant systems to identify the specific merchant context.
     */
    public static final String X_MERCHANT_ID = "X-Merchant-ID";

    /**
     * User ID header identifying the specific user making the request.
     * Used for user-level authorization and logging.
     */
    public static final String X_USER_ID = "X-User-ID";

    /**
     * Identity ID header for additional identity verification.
     * Can represent a user's identity across different contexts or systems.
     */
    public static final String X_IDENTITY_ID = "X-Identity-ID";

    /**
     * Standard HTTP Authorization header for authentication credentials.
     * Typically contains Bearer tokens, Basic auth, or other authentication schemes.
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * Unmodifiable map containing all header constants for easy iteration or lookup.
     * Useful for validation, logging, or dynamic processing of headers.
     */
    public static final Map<String, String> ALL_HEADERS;

    static {
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("X_REQUEST_ID", X_REQUEST_ID);
        headersMap.put("X_CORRELATION_ID", X_CORRELATION_ID);
        headersMap.put("X_MERCHANT_ID", X_MERCHANT_ID);
        headersMap.put("X_USER_ID", X_USER_ID);
        headersMap.put("X_IDENTITY_ID", X_IDENTITY_ID);
        headersMap.put("AUTHORIZATION", AUTHORIZATION);
        ALL_HEADERS = Collections.unmodifiableMap(headersMap);
    }

    /**
     * Checks if a given header name is one of the defined constants.
     *
     * @param headerName the header name to check (case-sensitive)
     * @return true if the header is defined in this class, false otherwise
     */
    public static boolean isDefinedHeader(String headerName) {
        return ALL_HEADERS.containsValue(headerName);
    }

    /**
     * Gets the constant name for a given header value.
     *
     * @param headerValue the header value to look up
     * @return the constant name if found, null otherwise
     */
    public static String getConstantName(String headerValue) {
        for (Map.Entry<String, String> entry : ALL_HEADERS.entrySet()) {
            if (entry.getValue().equals(headerValue)) {
                return entry.getKey();
            }
        }
        return null;
    }
}