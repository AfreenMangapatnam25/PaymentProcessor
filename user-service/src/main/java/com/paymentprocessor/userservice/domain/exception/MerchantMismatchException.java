package com.paymentprocessor.userservice.domain.exception;

import com.paymentprocessor.userservice.common.exception.ForbiddenException;

/**
 * Raised when an operation targets a resource outside the caller's merchant
 * scope. Reads normally surface as not-found (to avoid leaking existence); this
 * is used where a stronger, explicit tenancy assertion is warranted.
 */
public class MerchantMismatchException extends ForbiddenException {

    public MerchantMismatchException() {
        super("Resource does not belong to the caller's merchant");
    }
}
