package com.paymentprocessor.userservice.application.command;

import com.paymentprocessor.userservice.domain.address.AddressType;
import com.paymentprocessor.userservice.domain.address.OwnerType;

/**
 * Command to add an address to a user or customer.
 */
public record CreateAddressCommand(
        OwnerType ownerType,
        String ownerId,
        AddressType addressType,
        String line1,
        String line2,
        String city,
        String region,
        String postalCode,
        String countryCode,
        boolean defaultAddress
) {
}
