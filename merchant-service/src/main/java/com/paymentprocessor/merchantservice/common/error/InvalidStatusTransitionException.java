package com.paymentprocessor.merchantservice.common.error;

import com.paymentprocessor.merchantservice.common.enums.MerchantStatus;

/** Thrown when an illegal merchant lifecycle transition is attempted. Maps to HTTP 409. */
public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(MerchantStatus from, MerchantStatus to) {
        super("Illegal merchant status transition: " + from + " -> " + to);
    }
}
