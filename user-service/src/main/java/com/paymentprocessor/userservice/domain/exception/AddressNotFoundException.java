package com.paymentprocessor.userservice.domain.exception;

import com.paymentprocessor.userservice.common.exception.ResourceNotFoundException;

/**
 * Raised when an address cannot be found within the given owner scope.
 */
public class AddressNotFoundException extends ResourceNotFoundException {

    public AddressNotFoundException(String addressId) {
        super("Address", addressId);
    }
}
