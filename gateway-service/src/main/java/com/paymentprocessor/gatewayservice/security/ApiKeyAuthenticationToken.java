package com.paymentprocessor.gatewayservice.security;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authentication token carrying a raw API key (before validation) or an
 * authenticated principal plus its granted authorities (after validation).
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String apiKey;
    private final Object principal;
    private final String merchantId;

    /** Unauthenticated form: only the presented key is known. */
    public ApiKeyAuthenticationToken(String apiKey) {
        super(null);
        this.apiKey = apiKey;
        this.principal = null;
        this.merchantId = null;
        setAuthenticated(false);
    }

    /** Authenticated form: principal + authorities resolved from the key store. */
    public ApiKeyAuthenticationToken(Object principal, String merchantId,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = null;
        this.principal = principal;
        this.merchantId = merchantId;
        setAuthenticated(true);
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getMerchantId() {
        return merchantId;
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
