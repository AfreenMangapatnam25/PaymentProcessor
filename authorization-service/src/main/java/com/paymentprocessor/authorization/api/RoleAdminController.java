package com.paymentprocessor.authorization.api;

import com.paymentprocessor.authorization.dto.access.PermissionRequest;
import com.paymentprocessor.authorization.dto.access.PermissionResponse;
import com.paymentprocessor.authorization.dto.access.PolicyRequest;
import com.paymentprocessor.authorization.dto.access.PolicyResponse;
import com.paymentprocessor.authorization.dto.access.RoleAssignmentRequest;
import com.paymentprocessor.authorization.dto.access.RoleAssignmentResponse;
import com.paymentprocessor.authorization.dto.access.RoleRequest;
import com.paymentprocessor.authorization.dto.access.RoleResponse;
import com.paymentprocessor.authorization.service.access.RbacAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Administrative API for governing roles, permissions, role assignments and policies.
 */
@Tag(name = "Authorization Administration",
        description = "Manage roles, permissions, assignments and ABAC policies")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RbacAdminService rbacAdminService;

    // ---- permissions ----

    @Operation(summary = "Create a permission")
    @PostMapping("/permissions")
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody PermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PermissionResponse.from(rbacAdminService.createPermission(request)));
    }

    @Operation(summary = "List permissions")
    @GetMapping("/permissions")
    public List<PermissionResponse> listPermissions() {
        return rbacAdminService.listPermissions().stream().map(PermissionResponse::from).toList();
    }

    // ---- roles ----

    @Operation(summary = "Create a role")
    @PostMapping("/roles")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RoleResponse.from(rbacAdminService.createRole(request)));
    }

    @Operation(summary = "List roles")
    @GetMapping("/roles")
    public List<RoleResponse> listRoles() {
        return rbacAdminService.listRoles().stream().map(RoleResponse::from).toList();
    }

    @Operation(summary = "Get a role by name")
    @GetMapping("/roles/{name}")
    public RoleResponse getRole(@PathVariable String name) {
        return RoleResponse.from(rbacAdminService.getRole(name));
    }

    @Operation(summary = "Grant a permission to a role")
    @PostMapping("/roles/{name}/permissions/{permission}")
    public RoleResponse grantPermission(@PathVariable String name, @PathVariable String permission) {
        return RoleResponse.from(rbacAdminService.grantPermission(name, permission));
    }

    @Operation(summary = "Revoke a permission from a role")
    @DeleteMapping("/roles/{name}/permissions/{permission}")
    public RoleResponse revokePermission(@PathVariable String name, @PathVariable String permission) {
        return RoleResponse.from(rbacAdminService.revokePermission(name, permission));
    }

    @Operation(summary = "Delete a (non-system) role")
    @DeleteMapping("/roles/{name}")
    public ResponseEntity<Void> deleteRole(@PathVariable String name) {
        rbacAdminService.deleteRole(name);
        return ResponseEntity.noContent().build();
    }

    // ---- assignments ----

    @Operation(summary = "Assign a role to an identity")
    @PostMapping("/assignments")
    public ResponseEntity<RoleAssignmentResponse> assignRole(@Valid @RequestBody RoleAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RoleAssignmentResponse.from(rbacAdminService.assignRole(request)));
    }

    @Operation(summary = "List an identity's role assignments")
    @GetMapping("/identities/{identityId}/assignments")
    public List<RoleAssignmentResponse> listAssignments(@PathVariable String identityId) {
        return rbacAdminService.listAssignments(identityId).stream()
                .map(RoleAssignmentResponse::from).toList();
    }

    @Operation(summary = "Revoke a role from an identity")
    @DeleteMapping("/identities/{identityId}/roles/{roleName}")
    public ResponseEntity<Void> revokeRole(@PathVariable String identityId, @PathVariable String roleName) {
        rbacAdminService.revokeRole(identityId, roleName);
        return ResponseEntity.noContent().build();
    }

    // ---- policies ----

    @Operation(summary = "Create an ABAC / conditional policy")
    @PostMapping("/policies")
    public ResponseEntity<PolicyResponse> createPolicy(@Valid @RequestBody PolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PolicyResponse.from(rbacAdminService.createPolicy(request)));
    }

    @Operation(summary = "Update a policy")
    @PutMapping("/policies/{id}")
    public PolicyResponse updatePolicy(@PathVariable UUID id, @Valid @RequestBody PolicyRequest request) {
        return PolicyResponse.from(rbacAdminService.updatePolicy(id, request));
    }

    @Operation(summary = "List policies")
    @GetMapping("/policies")
    public List<PolicyResponse> listPolicies() {
        return rbacAdminService.listPolicies().stream().map(PolicyResponse::from).toList();
    }

    @Operation(summary = "Delete a policy")
    @DeleteMapping("/policies/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable UUID id) {
        rbacAdminService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
