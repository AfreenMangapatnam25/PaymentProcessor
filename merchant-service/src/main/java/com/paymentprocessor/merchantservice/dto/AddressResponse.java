package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.AddressType;
import java.util.UUID;

public record AddressResponse(
        UUID id, UUID merchantId, AddressType addressType, String line1, String line2,
        String city, String region, String postalCode, String country, boolean primary
) {}
