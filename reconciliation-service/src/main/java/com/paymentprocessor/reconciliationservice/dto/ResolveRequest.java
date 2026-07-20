package com.paymentprocessor.reconciliationservice.dto;

import com.paymentprocessor.reconciliationservice.domain.ResolutionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Resolve an exception with a resolution type and note. */
public record ResolveRequest(
        @NotNull ResolutionType resolutionType,
        @Size(max = 1024) String note,
        @NotBlank @Size(max = 128) String resolvedBy
) {
}
