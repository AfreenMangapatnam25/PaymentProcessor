package com.paymentprocessor.merchantservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.merchantservice.common.error.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads an API key from the {@code Authorization: Bearer <token>} header (or {@code X-API-Key})
 * and, if valid, populates the security context. Invalid credentials yield a 401 with a
 * structured {@link ApiError} body. Absent credentials pass through for the authorization layer
 * to decide (public endpoints remain reachable).
 *
 * <p><b>Coexistence with JWT bearer tokens.</b> This service accepts two credential types on the
 * same {@code Authorization: Bearer} header: long-lived merchant/admin <i>API keys</i> (handled
 * here) and short-lived RS256 <i>JWT access tokens</i> issued by the authentication service
 * (handled by Spring Security's resource-server filter — see
 * {@code config.SecurityConfig}). Because both would otherwise try to consume the same header,
 * this filter deliberately ignores anything that is shaped like a JWT and lets the resource
 * server claim it. See {@link #looksLikeJwt(String)}.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyAuthenticator authenticator;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(ApiKeyAuthenticator authenticator, ObjectMapper objectMapper) {
        this.authenticator = authenticator;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                authenticator.authenticate(token, clientIp(request))
                        .ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));
            } catch (BadCredentialsException ex) {
                writeUnauthorized(request, response, ex.getMessage());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the API key from the request, or returns {@code null} if this request carries no
     * API key for us to handle.
     *
     * <p>A {@code Bearer} value that looks like a JWT is skipped, so the OAuth2 resource-server
     * filter can authenticate it instead. {@code X-API-Key} is always treated as an API key —
     * that header is unambiguous.
     *
     * @param request the inbound request
     * @return the API key to authenticate, or {@code null} when there is none (or the credential
     *         belongs to the JWT filter)
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            // Defer JWTs to the resource server rather than failing them as bad API keys.
            return looksLikeJwt(token) ? null : token;
        }
        return request.getHeader("X-API-Key");
    }

    /**
     * Cheap structural test for a JWS compact serialisation: three dot-separated,
     * non-empty segments ({@code header.payload.signature}).
     *
     * <p>This is intentionally a shape check, not a validation — the only decision being made is
     * "which filter owns this credential". Actual signature, issuer, expiry and {@code purpose}
     * verification is the resource server's job. API keys issued by this service are opaque
     * random strings and never contain two dots, so the two namespaces do not overlap.
     *
     * @param token the raw bearer value
     * @return true if the value is shaped like a JWT and should be left to the resource server
     */
    private boolean looksLikeJwt(String token) {
        int firstDot = token.indexOf('.');
        if (firstDot <= 0) {
            return false;
        }
        int secondDot = token.indexOf('.', firstDot + 1);
        // Require a non-empty payload segment and something after the second dot.
        return secondDot > firstDot + 1 && secondDot < token.length() - 1;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(), "unauthorized", message, request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), error);
    }
}
