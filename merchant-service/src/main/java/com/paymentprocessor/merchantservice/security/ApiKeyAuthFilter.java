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

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return request.getHeader("X-API-Key");
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
