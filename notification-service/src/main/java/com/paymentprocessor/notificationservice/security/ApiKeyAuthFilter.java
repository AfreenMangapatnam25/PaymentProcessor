package com.paymentprocessor.notificationservice.security;

import com.paymentprocessor.notificationservice.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lightweight service-to-service auth: upstream services (payment-service,
 * ledger-service, merchant-service, settlement-service, dispute-service) must
 * present X-Internal-Api-Key on every /api/** call. Health/metrics stay open
 * for the orchestrator's liveness checks.
 *
 * This is intentionally not Spring Security -- the service only needs a
 * single shared secret between trusted internal callers, not user auth,
 * sessions, or roles. If that changes, replace this with Spring Security.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Api-Key";

    private final SecurityProperties securityProperties;

    public ApiKeyAuthFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || securityProperties.getInternalApiKey().isBlank();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (provided == null || !constantTimeEquals(provided, securityProperties.getInternalApiKey())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "missing or invalid " + HEADER);
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] x = a.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] y = b.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(x, y);
    }
}
