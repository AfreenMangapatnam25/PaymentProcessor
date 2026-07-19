package com.paymentprocessor.userservice.api.request;

import com.paymentprocessor.userservice.domain.address.AddressType;
import com.paymentprocessor.userservice.domain.address.OwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to create an address for a user or customer.
 */
public record CreateAddressRequest(

        @NotNull
        OwnerType ownerType,

        @NotBlank
        String ownerId,

        AddressType addressType,

        @NotBlank
        @Size(max = 255)
        String line1,

        @Size(max = 255)
        String line2,

        @NotBlank
        @Size(max = 128)
        String city,

        @Size(max = 128)
        String region,

        @NotBlank
        @Size(max = 32)
        String postalCode,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be an ISO-3166-1 alpha-2 code")
        String countryCode,

        boolean defaultAddress
) {
}
