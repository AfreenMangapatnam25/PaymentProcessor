package com.paymentprocessor.analytics.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Stateless OAuth2 <em>resource server</em> security for the analytics/reporting service.
 *
 * <p>How this fits the platform-wide authentication flow:</p>
 * <ol>
 *   <li>A user, merchant operator or back-office admin signs in at
 *       <strong>authentication-service</strong> (port 8081) using a password or a social
 *       identity provider.</li>
 *   <li>authentication-service issues an RS256-signed JWT access token carrying
 *       {@code sub}/{@code identity_id}, {@code principal_type}, a space-delimited
 *       {@code scope} claim, {@code purpose}, {@code sid} and {@code amr}, and publishes
 *       its public keys at {@code /.well-known/jwks.json}.</li>
 *   <li>The caller presents that token here as {@code Authorization: Bearer ...}; this
 *       class wires the chain that pulls the JWKS, verifies signature and issuer, and
 *       converts claims into authorities.</li>
 *   <li>Only then will the report/export endpoints hand back generated reports or
 *       pre-signed download URLs.</li>
 * </ol>
 *
 * <p>Reports aggregate merchant financial data out of the ClickHouse OLAP replica, so an
 * unauthenticated caller must never reach them. This service verifies tokens only - it
 * never issues them and stores no credentials.</p>
 *
 * <p>Note on the {@code purpose} claim: authentication-service sets {@code purpose=access}
 * on access tokens (as opposed to refresh or step-up tokens). Enforcing that value
 * belongs in a dedicated {@code OAuth2TokenValidator} layered onto the {@code JwtDecoder};
 * it is intentionally not done here so that the auto-configured decoder built from
 * {@code spring.security.oauth2.resourceserver.jwt.*} stays in force.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Claim distinguishing access tokens from refresh/step-up tokens. */
    private static final String PURPOSE_CLAIM = "purpose";

    /** The only {@code purpose} value this API accepts. */
    private static final String PURPOSE_ACCESS = "access";

    /** JWKS endpoint of the authentication service; keys are cached and rotated automatically. */
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /** Expected {@code iss} claim. Rejecting foreign issuers stops token-confusion attacks. */
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    /**
     * Endpoints deliberately left open: container/orchestrator probes and the Prometheus
     * scrape (which run before any identity exists), the OpenAPI documents and Swagger UI
     * that this service publishes via springdoc, and Spring's internal {@code /error}
     * dispatch - protecting {@code /error} would mask genuine 4xx/5xx responses behind a
     * second authentication failure.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/prometheus",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error"
    };

    /**
     * Production/default chain: everything except {@link #PUBLIC_ENDPOINTS} requires a
     * valid bearer token.
     *
     * <p>{@code matchIfMissing = true} makes this the chain you get unless a config file
     * explicitly opts out, so a missing property fails closed instead of open.</p>
     *
     * @param http                       the {@link HttpSecurity} builder supplied by Spring Security
     * @param jwtAuthenticationConverter converter mapping JWT claims onto authorities
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the filter chain cannot be built
     */
    @Bean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http,
                                                      JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http
                // Bearer tokens, not cookies: there is no session for a CSRF attack to ride,
                // and CSRF tokens would break programmatic clients and scheduled report jobs.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Deny by default, so newly added report endpoints are protected
                        // the moment they exist rather than by remembering to list them.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    /**
     * Development-only chain: permits every request.
     *
     * <p><strong>DEV ONLY - must never be active in a shared or production
     * environment.</strong> It is chosen only on an explicit
     * {@code security.jwt.enabled=false}, which this repo sets solely in the
     * {@code local} profile document of {@code application.yml} so the seeded report jobs
     * and Swagger UI can be exercised without running authentication-service.</p>
     *
     * @param http the {@link HttpSecurity} builder supplied by Spring Security
     * @return a permit-all {@link SecurityFilterChain}
     * @throws Exception if the filter chain cannot be built
     */
    @Bean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "false")
    public SecurityFilterChain permitAllFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Translates authentication-service's claims into the authorities that
     * {@code @PreAuthorize} and {@code hasAuthority(...)} evaluate.
     *
     * <ul>
     *   <li><b>scopes</b> - the space-delimited {@code scope} claim is split by
     *       {@link JwtGrantedAuthoritiesConverter} into {@code SCOPE_<value>} authorities
     *       (e.g. {@code reports:read} becomes {@code SCOPE_reports:read}). Scopes express
     *       what the token is permitted to do.</li>
     *   <li><b>principal type</b> - the custom {@code principal_type} claim
     *       (USER|MERCHANT|ADMIN|SERVICE) becomes a single {@code ROLE_<TYPE>} authority,
     *       so {@code principal_type=ADMIN} satisfies {@code hasRole('ADMIN')}. The role
     *       expresses what kind of caller is asking.</li>
     * </ul>
     *
     * <p>Both dimensions matter here: a MERCHANT token and an ADMIN token may share the
     * {@code reports:read} scope while being entitled to very different row sets.</p>
     *
     * @return a converter producing scope- and principal-type-derived authorities
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        scopes.setAuthorityPrefix("SCOPE_");
        scopes.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> mergeAuthorities(jwt, scopes.convert(jwt)));
        // Report ownership and audit trails key off identity_id, so expose that as the
        // principal name instead of the raw `sub`.
        converter.setPrincipalClaimName("identity_id");
        return converter;
    }

    /**
     * Combines the scope-derived authorities with the {@code principal_type} role.
     *
     * @param jwt         the validated token
     * @param scopeGrants authorities already derived from the {@code scope} claim; may be {@code null}
     * @return the full authority collection for the authenticated request
     */
    private Collection<GrantedAuthority> mergeAuthorities(Jwt jwt, Collection<GrantedAuthority> scopeGrants) {
        Collection<GrantedAuthority> authorities =
                new ArrayList<>(scopeGrants == null ? List.of() : scopeGrants);
        String principalType = jwt.getClaimAsString("principal_type");
        if (StringUtils.hasText(principalType)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + principalType.trim().toUpperCase()));
        }
        return authorities;
    }

    /**
     * Builds the decoder that validates tokens against the authentication service's JWKS.
     *
     * <p><b>Why this bean is declared explicitly.</b> Two reasons, one of them a bug fix:
     * <ol>
     *   <li>Spring Boot's {@code OAuth2ResourceServerJwtConfiguration} publishes a decoder per
     *       configured property - one for {@code jwk-set-uri}, one for {@code issuer-uri}. With
     *       both set it can create <i>two</i> {@code JwtDecoder} beans, and startup then fails
     *       with "required a single bean, but 2 were found". Those auto-configured beans are
     *       {@code @ConditionalOnMissingBean}, so declaring our own suppresses both.</li>
     *   <li>It lets us enforce the {@code purpose} claim. The authentication service issues
     *       refresh tokens and MFA step-up tickets from the same key pair; neither must ever be
     *       accepted as an API credential, so anything other than {@code purpose=access} is
     *       rejected alongside the standard expiry/not-before and issuer checks.</li>
     * </ol>
     * RS256 is pinned so a token cannot downgrade its own algorithm via its header.
     *
     * @return a {@link NimbusJwtDecoder} that caches and refreshes JWKS keys automatically
     */
    @Bean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true", matchIfMissing = true)
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        OAuth2TokenValidator<Jwt> purposeIsAccess =
                new JwtClaimValidator<String>(PURPOSE_CLAIM, PURPOSE_ACCESS::equals);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri), purposeIsAccess));
        return decoder;
    }
}
