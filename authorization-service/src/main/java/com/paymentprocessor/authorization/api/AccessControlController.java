package com.paymentprocessor.authorization.api;

import com.paymentprocessor.authorization.dto.access.AccessCheckRequest;
import com.paymentprocessor.authorization.dto.access.AccessDecisionResponse;
import com.paymentprocessor.authorization.dto.access.EffectivePermissionsResponse;
import com.paymentprocessor.authorization.dto.access.ScopeValidationRequest;
import com.paymentprocessor.authorization.service.access.AccessDecisionService;
import com.paymentprocessor.authorization.service.access.PermissionResolver;
import com.paymentprocessor.authorization.service.access.ScopeValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Access-control decision API: RBAC/ABAC access checks, scope validation and effective-permission
 * lookup. Called by the API Gateway and other services for per-request authorization.
 */
@Tag(name = "Access Control", description = "RBAC/ABAC access decisions, scope validation and permissions")
@RestController
@RequestMapping("/api/v1/access")
@RequiredArgsConstructor
public class AccessControlController {

    private final AccessDecisionService accessDecisionService;
    private final ScopeValidationService scopeValidationService;
    private final PermissionResolver permissionResolver;

    @Operation(summary = "Render an access decision (ALLOW / DENY / CHALLENGE)")
    @PostMapping("/check")
    public AccessDecisionResponse check(@Valid @RequestBody AccessCheckRequest request) {
        return accessDecisionService.decide(request);
    }

    @Operation(summary = "Validate that granted scopes satisfy the required scopes")
    @PostMapping("/scopes/validate")
    public Map<String, Boolean> validateScopes(@Valid @RequestBody ScopeValidationRequest request) {
        Set<String> granted = request.grantedScopes() == null ? Set.of() : new HashSet<>(request.grantedScopes());
        boolean valid = scopeValidationService.hasRequiredScopes(
                granted, new HashSet<>(request.requiredScopes()));
        return Map.of("valid", valid);
    }

    @Operation(summary = "Get the effective permission set for an identity")
    @GetMapping("/identities/{identityId}/permissions")
    public EffectivePermissionsResponse effectivePermissions(@PathVariable String identityId) {
        return new EffectivePermissionsResponse(
                identityId, permissionResolver.assignmentPermissions(identityId));
    }
}
