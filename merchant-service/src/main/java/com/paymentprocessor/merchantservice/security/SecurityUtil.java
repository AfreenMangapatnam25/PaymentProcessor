package com.paymentprocessor.merchantservice.security;

import com.paymentprocessor.merchantservice.common.error.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/** Helpers for reading the authenticated principal and enforcing merchant-scoped access. */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static Optional<MerchantPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof ApiKeyAuthenticationToken token) {
            return Optional.of(token.getPrincipal());
        }
        return Optional.empty();
    }

    /**
     * Ensures the caller may act on the given merchant. Admin principals may access any merchant;
     * a merchant principal may only access its own. To avoid leaking existence, a mismatch is
     * reported as not-found rather than forbidden.
     */
    public static void assertMerchantAccess(UUID merchantId) {
        MerchantPrincipal principal = currentPrincipal()
                .orElseThrow(() -> new AccessDeniedException("Unauthenticated"));
        if (principal.admin()) {
            return;
        }
        if (!merchantId.equals(principal.merchantId())) {
            throw ResourceNotFoundException.of("Merchant", merchantId);
        }
    }

    public static boolean isAdmin() {
        return currentPrincipal().map(MerchantPrincipal::admin).orElse(false);
    }
}
