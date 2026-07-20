package com.paymentprocessor.authorization.dto.payment;

/**
 * Request to reverse (void) an authorization hold before capture.
 */
public record ReversalRequest(String reason) {
}
