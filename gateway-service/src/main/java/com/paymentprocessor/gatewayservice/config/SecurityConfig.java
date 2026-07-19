package com.paymentprocessor.gatewayservice.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;

import com.paymentprocessor.gatewayservice.security.ApiKeyAuthenticationConverter;
import com.paymentprocessor.gatewayservice.security.ApiKeyAuthenticationManager;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive security for the gateway. Every request must present either a valid
 * short-lived JWT (validated locally against a cached JWKS) or a valid API key.
 * A small allow-list of paths (health, fallbacks) is public.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final GatewayProperties properties;

    public SecurityConfig(GatewayProperties properties) {
        this.properties = properties;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        String[] publicPaths = properties.getPublicPaths().toArray(String[]::new);

        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)         // stateless, token-based
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange(exchange -> exchange
                .pathMatchers(publicPaths).permitAll()
                .pathMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .anyExchange().authenticated())
            // JWT resource server: tokens are verified against a cached JWK set.
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            // API-key authentication runs alongside JWT; whichever matches wins.
            .addFilterAt(apiKeyAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((exchange, denied) ->
                        writeError(exchange, HttpStatus.UNAUTHORIZED, "authentication_required",
                                "A valid JWT or API key is required."))
                .accessDeniedHandler((exchange, denied) ->
                        writeError(exchange, HttpStatus.FORBIDDEN, "access_denied",
                                "You do not have access to this resource.")));

        return http.build();
    }

    /** API-key filter: stateless, no session persistence. */
    private AuthenticationWebFilter apiKeyAuthenticationFilter() {
        ReactiveAuthenticationManager manager = new ApiKeyAuthenticationManager(properties);
        AuthenticationWebFilter filter = new AuthenticationWebFilter(manager);
        filter.setServerAuthenticationConverter(new ApiKeyAuthenticationConverter(properties));
        filter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        return filter;
    }

    /** Maps JWT {@code roles}/{@code scope} claims to Spring authorities. */
    private ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt ->
                Flux.fromIterable(extractAuthorities(jwt)));
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Object roles = jwt.getClaims().get("roles");
        if (roles instanceof Collection<?> roleColl) {
            for (Object role : roleColl) {
                String r = String.valueOf(role);
                authorities.add(new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r));
            }
        }
        String scope = jwt.getClaimAsString("scope");
        if (scope == null) {
            scope = jwt.getClaimAsString("scp");
        }
        if (scope != null && !scope.isBlank()) {
            for (String s : scope.split("\\s+")) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
            }
        }
        return authorities;
    }

    /** Consistent JSON error envelope for auth failures. */
    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status,
            String error, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        String body = "{\"error\":\"" + error + "\",\"message\":\"" + message
                + "\",\"status\":" + status.value()
                + ",\"correlationId\":\"" + (correlationId == null ? "" : correlationId) + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
