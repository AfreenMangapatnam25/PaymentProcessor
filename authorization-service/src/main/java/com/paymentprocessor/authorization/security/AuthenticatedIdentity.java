package com.paymentprocessor.authorization.security;

import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.Set;

/**
 * Immutable representation of the caller resolved from a verified identity token.
 */
@Value
@Builder
public class AuthenticatedIdentity {

    /** Unique identity id (subject claim). */
    String id;

    /** Roles granted at authentication time. */
    Set<String> roles;

    /** OAuth2 / API scopes granted at authentication time. */
    Set<String> scopes;

    /** Merchant id for merchant-scoped identities; null for platform/user identities. */
    String merchantId;

    /** All raw claims, available to ABAC policy evaluation. */
    Map<String, Object> claims;

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasScope(String scope) {
        return scopes != null && scopes.contains(scope);
    }
}
