package com.paymentprocessor.userservice.domain.exception;

import com.paymentprocessor.userservice.common.constants.ErrorCode;
import com.paymentprocessor.userservice.common.exception.ConflictException;

/**
 * Raised when a customer would violate per-merchant uniqueness (external
 * reference or email). Messages omit the offending value to avoid PII leakage.
 */
public class DuplicateCustomerException extends ConflictException {

    public DuplicateCustomerException(String reason) {
        super(ErrorCode.CUSTOMER_ALREADY_EXISTS, reason);
    }

    public static DuplicateCustomerException externalRef() {
        return new DuplicateCustomerException("A customer with this external reference already exists for this merchant");
    }

    public static DuplicateCustomerException email() {
        return new DuplicateCustomerException("A customer with this email already exists for this merchant");
    }
}
