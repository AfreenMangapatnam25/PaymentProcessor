package com.paymentprocessor.authenticationservice.security;

import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import java.util.List;

/** Authenticated principal derived from a verified access token. */
public record AuthPrincipal(String identityId, PrincipalType principalType, List<String> scopes) {
}
