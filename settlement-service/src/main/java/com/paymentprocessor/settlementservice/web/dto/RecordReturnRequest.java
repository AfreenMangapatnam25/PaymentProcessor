package com.paymentprocessor.settlementservice.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Request to record a bank return against a payout. */
public record RecordReturnRequest(
        @NotBlank String reasonCode,
        String reasonDescription
) {
}
