package com.paymentprocessor.authenticationservice.security;

import com.nimbusds.jwt.JWTClaimsSet;
import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Validates the {@code Authorization: Bearer <jwt>} header for protected endpoints
 * and populates the SecurityContext. Only tokens whose {@code purpose} is
 * {@code access} are accepted; MFA-pending tickets are rejected here.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                JWTClaimsSet claims = jwtService.verify(token);
                if (!JwtService.PURPOSE_ACCESS.equals(jwtService.purpose(claims))) {
                    throw new IllegalStateException("Non-access token presented");
                }
                PrincipalType type = PrincipalType.valueOf(
                        String.valueOf(claims.getClaim("principal_type")));
                List<String> scopes = parseScopes(claims);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + type.name()));
                scopes.forEach(s -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + s)));

                AuthPrincipal principal = new AuthPrincipal(claims.getSubject(), type, scopes);
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private static List<String> parseScopes(JWTClaimsSet claims) {
        Object scope = claims.getClaim("scope");
        if (scope == null || scope.toString().isBlank()) {
            return List.of();
        }
        return Arrays.stream(scope.toString().split(" ")).filter(s -> !s.isBlank()).toList();
    }
}
