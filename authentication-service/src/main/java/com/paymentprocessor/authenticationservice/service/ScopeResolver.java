package com.paymentprocessor.authenticationservice.service;

import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import org.springframework.stereotype.Component;

import java.util.List;

/** Maps a principal type to the default scopes embedded in its access tokens. */
@Component
public class ScopeResolver {

    public List<String> defaultScopes(PrincipalType type) {
        return switch (type) {
            case USER -> List.of("user:self");
            case MERCHANT -> List.of("merchant:self");
            case ADMIN -> List.of("admin", "user:read", "merchant:read");
            case SERVICE -> List.of("service");
        };
    }
}
