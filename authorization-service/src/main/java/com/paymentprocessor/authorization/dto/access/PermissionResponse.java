package com.paymentprocessor.authorization.dto.access;

import com.paymentprocessor.authorization.domain.access.Permission;

import java.util.UUID;

public record PermissionResponse(UUID id, String name, String resource, String action, String description) {
    public static PermissionResponse from(Permission p) {
        return new PermissionResponse(p.getId(), p.getName(), p.getResource(), p.getAction(), p.getDescription());
    }
}
