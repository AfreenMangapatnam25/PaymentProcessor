package com.paymentprocessor.authorization.dto.access;

import com.paymentprocessor.authorization.domain.access.Permission;
import com.paymentprocessor.authorization.domain.access.Role;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record RoleResponse(
        UUID id,
        String name,
        String description,
        String category,
        Set<String> permissions,
        String parentRole,
        boolean systemRole
) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getCategory(),
                role.getPermissions().stream().map(Permission::getName).collect(Collectors.toSet()),
                role.getParent() != null ? role.getParent().getName() : null,
                role.isSystemRole());
    }
}
