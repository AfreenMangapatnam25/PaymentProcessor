package com.paymentprocessor.authorization.dto.access;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record RoleRequest(
        @NotBlank String name,
        String description,
        @NotBlank String category,
        List<String> permissions,
        String parentRole,
        boolean systemRole
) {
}
