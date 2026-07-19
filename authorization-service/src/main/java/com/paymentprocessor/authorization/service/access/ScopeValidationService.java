package com.paymentprocessor.authorization.service.access;

import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Validates that OAuth2 / API scopes granted at authentication time authorize the current request,
 * supporting hierarchical wildcard scopes (e.g. {@code payments:*} satisfies {@code payments:write}).
 */
@Service
public class ScopeValidationService {

    /** @return true when every required scope is satisfied by the granted scopes. */
    public boolean hasRequiredScopes(Set<String> grantedScopes, Set<String> requiredScopes) {
        if (requiredScopes == null || requiredScopes.isEmpty()) {
            return true;
        }
        if (grantedScopes == null || grantedScopes.isEmpty()) {
            return false;
        }
        return requiredScopes.stream().allMatch(required -> isGranted(grantedScopes, required));
    }

    private boolean isGranted(Set<String> granted, String required) {
        if (granted.contains(required) || granted.contains("*")) {
            return true;
        }
        int idx = required.indexOf(':');
        if (idx > 0) {
            String wildcard = required.substring(0, idx) + ":*";
            return granted.contains(wildcard);
        }
        return false;
    }
}
