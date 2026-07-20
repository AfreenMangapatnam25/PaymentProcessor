package com.paymentprocessor.settlementservice.web.dto;

import com.paymentprocessor.settlementservice.enums.ApprovalLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request to approve a pending adjustment at a given authority level. */
public record ApproveAdjustmentRequest(
        @NotBlank String approvedBy,
        @NotNull ApprovalLevel approverLevel
) {
}
