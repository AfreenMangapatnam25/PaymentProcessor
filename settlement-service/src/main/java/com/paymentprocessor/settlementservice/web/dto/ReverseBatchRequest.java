package com.paymentprocessor.settlementservice.web.dto;

import com.paymentprocessor.settlementservice.enums.ApprovalLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request to reverse a settled batch and recover funds. */
public record ReverseBatchRequest(
        @NotBlank String reason,
        @NotBlank String approvedBy,
        @NotNull ApprovalLevel approverLevel
) {
}
