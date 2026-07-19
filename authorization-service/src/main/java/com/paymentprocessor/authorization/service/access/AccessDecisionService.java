package com.paymentprocessor.authorization.service.access;

import com.paymentprocessor.authorization.domain.enums.AccessDecision;
import com.paymentprocessor.authorization.dto.access.AccessCheckRequest;
import com.paymentprocessor.authorization.dto.access.AccessDecisionResponse;
import com.paymentprocessor.authorization.exception.UnauthenticatedException;
import com.paymentprocessor.authorization.security.AuthenticatedIdentity;
import com.paymentprocessor.authorization.security.IdentityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The service's decision point. Combines RBAC (effective permissions), resource-ownership checks,
 * OAuth2 scope validation and the ABAC policy engine into a single {@code ALLOW / DENY / CHALLENGE}
 * verdict with deny-by-default conflict resolution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessDecisionService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final PermissionResolver permissionResolver;
    private final ScopeValidationService scopeValidationService;
    private final PolicyEvaluationService policyEvaluationService;

    public AccessDecisionResponse decide(AccessCheckRequest request) {
        Optional<AuthenticatedIdentity> caller = IdentityContext.current();
        String identityId = request.identityId() != null
                ? request.identityId()
                : caller.map(AuthenticatedIdentity::getId).orElse(null);
        if (identityId == null) {
            throw new UnauthenticatedException("No identity supplied and no authenticated caller present");
        }

        boolean callerIsSubject = caller.map(c -> identityId.equals(c.getId())).orElse(false);
        Set<String> tokenRoles = callerIsSubject ? nullSafe(caller.get().getRoles()) : Set.of();
        Set<String> tokenScopes = callerIsSubject ? nullSafe(caller.get().getScopes()) : Set.of();
        String merchantId = callerIsSubject ? caller.get().getMerchantId() : null;

        // 1. Scope validation.
        if (request.requiredScopes() != null && !request.requiredScopes().isEmpty()
                && !scopeValidationService.hasRequiredScopes(tokenScopes, new HashSet<>(request.requiredScopes()))) {
            return deny(request, identityId, "INSUFFICIENT_SCOPE",
                    "Token is missing one or more required scopes", List.of());
        }

        Set<String> permissions = permissionResolver.effectivePermissions(identityId, tokenRoles);
        boolean rbacAllow = permissionResolver.grants(permissions, request.resource(), request.action());

        // 2. Resource ownership (resource-level authorization).
        if (request.resourceOwnerId() != null) {
            boolean owns = request.resourceOwnerId().equals(identityId)
                    || (merchantId != null && request.resourceOwnerId().equals(merchantId));
            boolean adminOverride = tokenRoles.contains(SUPER_ADMIN)
                    || permissionResolver.grants(permissions, request.resource(), "admin")
                    || permissionResolver.grants(permissions, "merchant", "admin");
            if (!owns && !adminOverride) {
                return deny(request, identityId, "CROSS_OWNER_DENIED",
                        "Caller may not access a resource owned by another identity", List.of());
            }
        }

        // 3. ABAC / conditional policy engine.
        Map<String, Object> context = buildContext(request, identityId, merchantId, tokenRoles);
        PolicyEvaluationService.PolicyEvaluationResult policy =
                policyEvaluationService.evaluate(request.resource(), request.action(), context);

        // 4. Conflict resolution (deny-by-default, explicit deny overrides).
        if (policy.explicitDeny()) {
            return build(AccessDecision.DENY, request, identityId, "POLICY_DENY",
                    "Denied by policy: " + policy.denyPolicy(), policy.evaluatedPolicies());
        }
        if (rbacAllow) {
            if (policy.stepUpRequired()) {
                return build(AccessDecision.CHALLENGE, request, identityId, "STEP_UP_REQUIRED",
                        "Step-up authentication required to proceed", policy.evaluatedPolicies());
            }
            return build(AccessDecision.ALLOW, request, identityId, "RBAC_ALLOW",
                    "Granted by role permissions", policy.evaluatedPolicies());
        }
        if (policy.explicitAllow()) {
            return build(AccessDecision.ALLOW, request, identityId, "POLICY_ALLOW",
                    "Granted by policy", policy.evaluatedPolicies());
        }
        return build(AccessDecision.DENY, request, identityId, "NO_PERMISSION",
                "Identity lacks permission for " + request.resource() + ":" + request.action(),
                policy.evaluatedPolicies());
    }

    private Map<String, Object> buildContext(AccessCheckRequest request, String identityId,
                                             String merchantId, Set<String> roles) {
        Map<String, Object> context = new HashMap<>();
        context.put("action", request.action());
        context.put("resource.type", request.resource());
        context.put("resource.owner", request.resourceOwnerId());
        context.put("subject.id", identityId);
        context.put("subject.merchant_id", merchantId);
        context.put("subject.roles", roles);
        if (request.scopeId() != null) {
            context.put("resource.scope_id", request.scopeId());
        }
        if (request.attributes() != null) {
            context.putAll(request.attributes());
        }
        return context;
    }

    private AccessDecisionResponse deny(AccessCheckRequest request, String identityId,
                                        String code, String reason, List<String> policies) {
        return build(AccessDecision.DENY, request, identityId, code, reason, policies);
    }

    private AccessDecisionResponse build(AccessDecision decision, AccessCheckRequest request,
                                         String identityId, String code, String reason,
                                         List<String> policies) {
        return AccessDecisionResponse.builder()
                .decision(decision)
                .reasonCode(code)
                .reason(reason)
                .identityId(identityId)
                .resource(request.resource())
                .action(request.action())
                .evaluatedPolicies(policies)
                .evaluatedAt(Instant.now())
                .build();
    }

    private static Set<String> nullSafe(Set<String> value) {
        return value == null ? Set.of() : value;
    }
}
