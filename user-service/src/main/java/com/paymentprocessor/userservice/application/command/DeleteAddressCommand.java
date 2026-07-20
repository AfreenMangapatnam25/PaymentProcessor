package com.paymentprocessor.userservice.application.command;

import com.paymentprocessor.userservice.domain.address.OwnerType;

/**
 * Command to soft-delete an address.
 */
public record DeleteAddressCommand(
        String addressId,
        OwnerType ownerType,
        String ownerId,
        long expectedVersion
) {
}
