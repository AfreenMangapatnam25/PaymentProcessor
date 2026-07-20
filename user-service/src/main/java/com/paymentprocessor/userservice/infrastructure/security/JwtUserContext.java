package com.paymentprocessor.userservice.infrastructure.security;

import com.paymentprocessor.userservice.infrastructure.config.AppProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Convenience accessor for identity/merchant claims on the current JWT. Used by
 * merchant-scoped operations (e.g. customers) to enforce tenant isolation
 * (rule 12). Claim names are configuration-driven.
 */
@Component
public class JwtUserContext {

    private final String merchantIdClaim;
    private final String identityIdClaim;

    public JwtUserContext(AppProperties properties) {
        AppProperties.Security.Jwt jwt = properties.security().jwt();
        this.merchantIdClaim = jwt.merchantIdClaim();
        this.identityIdClaim = jwt.identityIdClaim();
    }

    public Optional<String> currentMerchantId() {
        return claim(merchantIdClaim);
    }

    public Optional<String> currentIdentityId() {
        return claim(identityIdClaim);
    }

    private Optional<String> claim(String name) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return Optional.ofNullable(jwtAuth.getToken().getClaimAsString(name));
        }
        return Optional.empty();
    }
}
