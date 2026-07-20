package com.paymentprocessor.merchantservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.merchantservice.common.error.ApiError;
import com.paymentprocessor.merchantservice.security.ApiKeyAuthFilter;
import com.paymentprocessor.merchantservice.security.ApiKeyAuthenticator;
import com.paymentprocessor.merchantservice.security.SecurityRoles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless API-key security. Public: health probes, OpenAPI/Swagger. Onboarding, lifecycle
 * transitions, provisioning and compliance callbacks require the platform admin role; all other
 * API operations require an authenticated key, with mutating verbs requiring the WRITE authority
 * (READ_ONLY keys are limited to GET).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter(ApiKeyAuthenticator authenticator, ObjectMapper objectMapper) {
        return new ApiKeyAuthFilter(authenticator, objectMapper);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ApiKeyAuthFilter apiKeyAuthFilter,
                                           ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public infrastructure endpoints
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Platform/admin-only operations (onboarding, lifecycle, compliance decisions, pricing)
                        .requestMatchers(HttpMethod.POST, "/api/v1/merchants").hasRole(SecurityRoles.ADMIN)
                        .requestMatchers("/api/v1/merchants/*/status").hasRole(SecurityRoles.ADMIN)
                        .requestMatchers("/api/v1/merchants/*/kyb/cases/*/decision").hasRole(SecurityRoles.ADMIN)
                        .requestMatchers("/api/v1/merchants/*/pricing-plan").hasRole(SecurityRoles.ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/merchants/*/fees").hasRole(SecurityRoles.ADMIN)
                        // Read vs write for the remaining merchant API
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(SecurityRoles.READ, SecurityRoles.ADMIN)
                        .requestMatchers("/api/**").hasAnyRole(SecurityRoles.WRITE, SecurityRoles.ADMIN)
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authEx) ->
                                writeError(objectMapper, response, request.getRequestURI(),
                                        HttpStatus.UNAUTHORIZED, "unauthorized",
                                        "Authentication required"))
                        .accessDeniedHandler((request, response, deniedEx) ->
                                writeError(objectMapper, response, request.getRequestURI(),
                                        HttpStatus.FORBIDDEN, "forbidden",
                                        "Insufficient privileges for this operation")))
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(ObjectMapper objectMapper, jakarta.servlet.http.HttpServletResponse response,
                            String path, HttpStatus status, String code, String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(status.value(), status.getReasonPhrase(), code, message, path);
        objectMapper.writeValue(response.getWriter(), error);
    }
}
