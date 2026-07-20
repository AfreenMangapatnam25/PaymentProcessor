package com.paymentprocessor.userservice.domain.exception;

import com.paymentprocessor.userservice.common.exception.ResourceNotFoundException;

/**
 * Raised when a customer cannot be found within the caller's merchant scope.
 * Note: a customer that exists for another merchant is reported as not-found,
 * never leaking cross-tenant existence (rule 12).
 */
public class CustomerNotFoundException extends ResourceNotFoundException {

    public CustomerNotFoundException(String customerId) {
        super("Customer", customerId);
    }
}
