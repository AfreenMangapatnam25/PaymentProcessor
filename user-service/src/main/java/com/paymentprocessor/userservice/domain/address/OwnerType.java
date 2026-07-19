package com.paymentprocessor.userservice.domain.address;

import com.paymentprocessor.userservice.domain.valueobject.CustomerId;
import com.paymentprocessor.userservice.domain.valueobject.UserId;

/**
 * The kind of subject an address belongs to. Also the crypto-key subject type,
 * so an address encrypts with its owner's DEK.
 */
public enum OwnerType {

    USER(UserId.PREFIX),
    CUSTOMER(CustomerId.PREFIX);

    private final String idPrefix;

    OwnerType(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public String idPrefix() {
        return idPrefix;
    }
}
