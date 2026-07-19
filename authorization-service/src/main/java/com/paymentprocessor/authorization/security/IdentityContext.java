package com.paymentprocessor.authorization.security;

import java.util.Optional;

/**
 * Thread-bound holder for the {@link AuthenticatedIdentity} of the in-flight request.
 * Populated by {@link JwtClaimsFilter} and cleared at the end of the request.
 */
public final class IdentityContext {

    private static final ThreadLocal<AuthenticatedIdentity> CURRENT = new ThreadLocal<>();

    private IdentityContext() {
    }

    public static void set(AuthenticatedIdentity identity) {
        CURRENT.set(identity);
    }

    public static Optional<AuthenticatedIdentity> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static AuthenticatedIdentity require() {
        AuthenticatedIdentity identity = CURRENT.get();
        if (identity == null) {
            throw new IllegalStateException("No authenticated identity bound to the current request");
        }
        return identity;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
