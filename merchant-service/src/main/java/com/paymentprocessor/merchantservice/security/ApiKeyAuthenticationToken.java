package com.paymentprocessor.merchantservice.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/** Authentication produced by {@link ApiKeyAuthFilter} once an API key has been validated. */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final transient MerchantPrincipal principal;

    public ApiKeyAuthenticationToken(MerchantPrincipal principal,
                                     Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;  // credentials are not retained after authentication
    }

    @Override
    public MerchantPrincipal getPrincipal() {
        return principal;
    }
}
