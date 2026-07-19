package com.paymentprocessor.userservice.application.query;

import com.paymentprocessor.userservice.domain.address.OwnerType;

/**
 * Query to fetch one address within its owner scope.
 */
public record GetAddressQuery(String addressId, OwnerType ownerType, String ownerId) {
}
