package com.paymentprocessor.authenticationservice.config;

import com.paymentprocessor.authenticationservice.security.JwtAuthenticationFilter;
import com.paymentprocessor.authenticationservice.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public discovery + health
                .requestMatchers(
                        "/.well-known/jwks.json",
                        "/actuator/health/**",
                        "/actuator/info",
                        "/actuator/prometheus",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html").permitAll()
                // Public auth flows
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/auth/login",
                        "/api/v1/auth/login/mfa",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout",
                        "/api/v1/auth/token/introspect",
                        "/api/v1/passwords/forgot",
                        "/api/v1/passwords/reset",
                        "/api/v1/verification/*/confirm").permitAll()
                // Service-to-service API-key verification (called by other services)
                .requestMatchers(HttpMethod.POST, "/api/v1/api-keys/verify").permitAll()
                // Admin surface
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // Everything else requires a valid access token
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(authenticationEntryPoint))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Argon2id with OWASP-aligned parameters (saltLen=16, hashLen=32, parallelism=1,
        // memory=1<<14 KiB = 16 MiB, iterations=3).
        return new Argon2PasswordEncoder(16, 32, 1, 1 << 14, 3);
    }
}
