package com.paymentprocessor.merchantservice.common.error;

/** Thrown when a requested resource does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String type, Object id) {
        return new ResourceNotFoundException(type + " not found: " + id);
    }
}
