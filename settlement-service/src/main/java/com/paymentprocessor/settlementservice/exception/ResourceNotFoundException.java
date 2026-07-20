package com.paymentprocessor.settlementservice.exception;

/** Thrown when a requested settlement resource does not exist. */
public class ResourceNotFoundException extends SettlementException {

    public ResourceNotFoundException(String resource, String id) {
        super("RESOURCE_NOT_FOUND", resource + " not found: " + id);
    }
}
