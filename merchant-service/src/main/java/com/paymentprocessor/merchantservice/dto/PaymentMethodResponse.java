package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.PaymentMethodType;
import java.util.UUID;

public record PaymentMethodResponse(
        UUID id, UUID merchantId, PaymentMethodType methodType, boolean enabled, String settingsJson
) {}
