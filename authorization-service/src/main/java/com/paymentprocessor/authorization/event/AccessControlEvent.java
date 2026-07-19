package com.paymentprocessor.authorization.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted for access-control changes ({@code RoleAssigned}, {@code PermissionGranted},
 * {@code PermissionRevoked}) consumed by the Audit service and others.
 */
@Builder
public record AccessControlEvent(
        UUID eventId,
        String eventType,
        String identityId,
        String roleName,
        String permissionName,
        String scope,
        String scopeId,
        Instant occurredAt
) {
    public static final class Types {
        public static final String ROLE_ASSIGNED = "RoleAssigned";
        public static final String ROLE_REVOKED = "RoleRevoked";
        public static final String PERMISSION_GRANTED = "PermissionGranted";
        public static final String PERMISSION_REVOKED = "PermissionRevoked";

        private Types() {
        }
    }
}
