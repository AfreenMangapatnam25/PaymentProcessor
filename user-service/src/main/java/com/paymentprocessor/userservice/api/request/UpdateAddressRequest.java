package com.paymentprocessor.userservice.api.request;

import com.paymentprocessor.userservice.domain.address.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Request to replace an address's contents and default flag. Owner is supplied
 * as query parameters so it cannot be spoofed via the body.
 */
public record UpdateAddressRequest(

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

        boolean defaultAddress,

        @NotNull
        @PositiveOrZero
        Long expectedVersion
) {
}
