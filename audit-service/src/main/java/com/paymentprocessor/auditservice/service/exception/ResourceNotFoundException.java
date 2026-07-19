package com.paymentprocessor.auditservice.service.exception;

/** Thrown when a requested audit record or batch does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
