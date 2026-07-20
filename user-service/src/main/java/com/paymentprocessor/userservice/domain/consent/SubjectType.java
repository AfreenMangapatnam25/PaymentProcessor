package com.paymentprocessor.userservice.domain.consent;

import com.paymentprocessor.userservice.domain.valueobject.CustomerId;
import com.paymentprocessor.userservice.domain.valueobject.UserId;

/**
 * The kind of subject a consent applies to.
 */
public enum SubjectType {

    USER(UserId.PREFIX),
    CUSTOMER(CustomerId.PREFIX);

    private final String idPrefix;

    SubjectType(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public String idPrefix() {
        return idPrefix;
    }
}
