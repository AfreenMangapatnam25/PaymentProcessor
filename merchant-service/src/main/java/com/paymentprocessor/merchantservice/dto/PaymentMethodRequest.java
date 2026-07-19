package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.PaymentMethodType;
import jakarta.validation.constraints.NotNull;

public record PaymentMethodRequest(
        @NotNull PaymentMethodType methodType,
        boolean enabled,
        String settingsJson
) {}
