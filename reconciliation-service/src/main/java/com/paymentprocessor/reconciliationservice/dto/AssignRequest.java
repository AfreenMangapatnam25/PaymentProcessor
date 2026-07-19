package com.paymentprocessor.reconciliationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Assign an exception to an analyst. */
public record AssignRequest(
        @NotBlank @Size(max = 128) String assignee
) {
}
