package com.paymentprocessor.userservice.infrastructure.security;

import com.paymentprocessor.userservice.infrastructure.config.AppProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

/**
 * Converts a validated JWT into an authentication token whose authorities come
 * from the configured scope claim (e.g. {@code scope} -> {@code SCOPE_*}). Claim
 * name and prefix are configuration-driven so the auth contract can change
 * without code edits.
 */
@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter authoritiesConverter;

    public JwtAuthenticationConverter(AppProperties properties) {
        AppProperties.Security.Jwt jwt = properties.security().jwt();
        this.authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        this.authoritiesConverter.setAuthorityPrefix(jwt.authoritiesPrefix());
        this.authoritiesConverter.setAuthoritiesClaimName(jwt.authoritiesClaim());
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, authoritiesConverter.convert(jwt), jwt.getSubject());
    }
}
