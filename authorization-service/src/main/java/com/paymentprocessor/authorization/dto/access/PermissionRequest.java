package com.paymentprocessor.authorization.dto.access;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequest(
        @NotBlank String name,
        @NotBlank String resource,
        @NotBlank String action,
        String description
) {
}
