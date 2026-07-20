package com.paymentprocessor.userservice.domain.exception;

import com.paymentprocessor.userservice.common.exception.ResourceNotFoundException;

/**
 * Raised when a user cannot be found by id or identity. Maps to HTTP 404 via
 * the {@link ResourceNotFoundException} hierarchy.
 */
public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(String userId) {
        super("User", userId);
    }

    public static UserNotFoundException byId(String userId) {
        return new UserNotFoundException(userId);
    }
}
