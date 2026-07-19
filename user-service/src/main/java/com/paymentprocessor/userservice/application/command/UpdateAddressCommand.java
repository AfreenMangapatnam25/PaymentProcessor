package com.paymentprocessor.userservice.application.command;

import com.paymentprocessor.userservice.domain.address.AddressType;
import com.paymentprocessor.userservice.domain.address.OwnerType;

/**
 * Command to replace an address's contents and default flag.
 */
public record UpdateAddressCommand(
        String addressId,
        OwnerType ownerType,
        String ownerId,
        AddressType addressType,
        String line1,
        String line2,
        String city,
        String region,
        String postalCode,
        String countryCode,
        boolean defaultAddress,
        long expectedVersion
) {
}
