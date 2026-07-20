package com.paymentprocessor.authorization.domain.enums;

/** Card security code (CVV/CVC) verification outcome. */
public enum CvvResult {
    MATCH, NO_MATCH, NOT_PROVIDED, UNAVAILABLE
}
