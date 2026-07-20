package com.paymentprocessor.ledgerservice.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * Standard error envelope returned for all non-2xx responses.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<String> details
) {
}
