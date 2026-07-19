package com.paymentprocessor.auditservice.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lightweight shared-secret authentication for internal service-to-service calls.
 * Callers present a key via {@code X-Api-Key}. Comparison is constant-time to avoid
 * timing side-channels. Actuator, docs and error paths are exempt.
 *
 * <p>In production this typically sits behind mTLS at the mesh/gateway layer; the API
 * key is defence in depth. Disable via {@code audit.security.enabled=false} for local dev.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String HEADER = "X-Api-Key";

    private final Set<String> apiKeys;

    public ApiKeyAuthFilter(Set<String> apiKeys) {
        this.apiKeys = new HashSet<>(apiKeys);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String presented = request.getHeader(HEADER);
        if (presented != null && matchesAny(presented)) {
            chain.doFilter(request, response);
            return;
        }
        log.warn("Rejected unauthenticated request to {} from {}",
                request.getRequestURI(), request.getRemoteAddr());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Missing or invalid API key\"}");
    }

    private boolean matchesAny(String presented) {
        byte[] presentedBytes = presented.getBytes(StandardCharsets.UTF_8);
        boolean matched = false;
        // Iterate over all keys (no short-circuit) to keep timing independent of position.
        for (String key : apiKeys) {
            if (MessageDigest.isEqual(presentedBytes, key.getBytes(StandardCharsets.UTF_8))) {
                matched = true;
            }
        }
        return matched;
    }
}
