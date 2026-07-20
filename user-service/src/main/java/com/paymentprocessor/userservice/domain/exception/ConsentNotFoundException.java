package com.paymentprocessor.userservice.domain.exception;

import com.paymentprocessor.userservice.common.exception.ResourceNotFoundException;

/**
 * Raised when a consent record cannot be found for the given subject and kind.
 */
public class ConsentNotFoundException extends ResourceNotFoundException {

    public ConsentNotFoundException(String reference) {
        super("Consent", reference);
    }
}
