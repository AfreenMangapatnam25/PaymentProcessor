package com.paymentprocessor.authorization.security;

import com.paymentprocessor.authorization.config.JwtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extracts identity claims from the bearer token on each request and binds an
 * {@link AuthenticatedIdentity} to the {@link IdentityContext}. Requests without a valid token
 * proceed with no bound identity; endpoints that require authentication enforce it downstream.
 */
@Component
@RequiredArgsConstructor
public class JwtClaimsFilter extends OncePerRequestFilter {

    private final JwtTokenDecoder decoder;
    private final JwtProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (token != null) {
                Map<String, Object> claims = decoder.decode(token);
                if (claims != null) {
                    IdentityContext.set(toIdentity(claims));
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            IdentityContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(properties.getHeader());
        if (header == null || header.isBlank()) {
            return null;
        }
        return header.regionMatches(true, 0, "Bearer ", 0, 7) ? header.substring(7).trim() : header.trim();
    }

    private AuthenticatedIdentity toIdentity(Map<String, Object> claims) {
        Object subject = claims.get(properties.getSubjectClaim());
        Object merchant = claims.get(properties.getMerchantClaim());
        return AuthenticatedIdentity.builder()
                .id(subject != null ? subject.toString() : null)
                .roles(asStringSet(claims.get(properties.getRolesClaim())))
                .scopes(asStringSet(claims.get(properties.getScopeClaim())))
                .merchantId(merchant != null ? merchant.toString() : null)
                .claims(claims)
                .build();
    }

    private Set<String> asStringSet(Object value) {
        Set<String> result = new LinkedHashSet<>();
        if (value == null) {
            return result;
        }
        if (value instanceof Collection<?> collection) {
            collection.forEach(v -> result.add(String.valueOf(v)));
        } else {
            for (String part : value.toString().split("[\\s,]+")) {
                if (!part.isBlank()) {
                    result.add(part);
                }
            }
        }
        return result;
    }
}
