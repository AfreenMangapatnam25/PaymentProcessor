package com.paymentprocessor.userservice.application.query;

import com.paymentprocessor.userservice.domain.address.OwnerType;

/**
 * Query to list an owner's live addresses.
 */
public record ListAddressesQuery(OwnerType ownerType, String ownerId) {
}
