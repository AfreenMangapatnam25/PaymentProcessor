package com.paymentprocessor.merchantservice.dto;

import com.paymentprocessor.merchantservice.common.enums.MerchantStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request an explicit merchant lifecycle transition. */
public record StatusChangeRequest(
        @NotNull MerchantStatus targetStatus,
        @Size(max = 512) String reason
) {}
