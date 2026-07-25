package com.paymentprocessor.reconciliationservice.config;

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
 * Stateless OAuth2 <em>resource server</em> security for reconciliation-service.
 *
 * <p>How this fits the platform-wide authentication flow:</p>
 * <ol>
 *   <li>A user (or an operator, or another service) authenticates at
 *       <strong>authentication-service</strong> on port 8081 - via password login or a
 *       social identity provider.</li>
 *   <li>authentication-service mints an RS256-signed JWT access token carrying
 *       {@code sub}/{@code identity_id}, {@code principal_type}, a space-delimited
 *       {@code scope} claim, {@code purpose}, {@code sid} and {@code amr}, and publishes
 *       the matching public keys at {@code /.well-known/jwks.json}.</li>
 *   <li>The caller sends that token to this service as {@code Authorization: Bearer ...}.
 *       This class configures the filter chain that fetches the JWKS, verifies the
 *       signature and issuer, and turns the token's claims into Spring Security
 *       authorities.</li>
 *   <li>Only then do the reconciliation controllers return matched/mismatched
 *       transaction data.</li>
 * </ol>
 *
 * <p>This service <em>never</em> issues tokens and holds no user credentials; it is a
 * pure verifier. Because tokens are self-contained and verified on every request there
 * is no server-side session - hence {@link SessionCreationPolicy#STATELESS} and disabled
 * CSRF (there is no browser cookie for an attacker to ride on).</p>
 *
 * <p>Two mutually exclusive filter chains are declared, selected by the
 * {@code security.jwt.enabled} property, so that a developer running the {@code local}
 * profile against seeded data does not need authentication-service on their machine.</p>
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
     * Endpoints that must stay reachable without a token: Kubernetes/Docker probes and
     * the Prometheus scrape (these run before any identity exists), the OpenAPI
     * documents, and Spring's internal {@code /error} dispatch - if {@code /error} were
     * protected, a failed request would turn a clean 401 into a confusing redirect loop.
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
     * Production/default chain: every request other than {@link #PUBLIC_ENDPOINTS} needs
     * a valid bearer token.
     *
     * <p>{@code matchIfMissing = true} means the secure chain is what you get unless
     * something explicitly opts out - failing closed rather than open if the property is
     * ever dropped from a config file.</p>
     *
     * @param http                      the {@link HttpSecurity} builder supplied by Spring Security
     * @param jwtAuthenticationConverter converter that maps JWT claims onto authorities
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the filter chain cannot be built
     */
    @Bean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http,
                                                      JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http
                // No cookies or server-side sessions are used, so CSRF tokens would protect
                // nothing while breaking every non-browser API client.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Deny-by-default: any endpoint added later is protected automatically
                        // rather than silently shipping unauthenticated.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    /**
     * Development-only chain: permits every request.
     *
     * <p><strong>DEV ONLY - never activate outside a developer machine.</strong> It is
     * selected only by an explicit {@code security.jwt.enabled=false}, which this repo
     * sets exclusively in the {@code local} profile document of {@code application.yml}
     * so that the Flyway-seeded reconciliation data can be browsed without running
     * authentication-service.</p>
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
     * Translates the claims minted by authentication-service into Spring Security
     * authorities, which is what {@code @PreAuthorize} and {@code hasAuthority(...)}
     * checks actually read.
     *
     * <p>Two independent mappings are produced:</p>
     * <ul>
     *   <li><b>scopes</b> - the space-delimited {@code scope} claim (OAuth2 standard
     *       form) is split by {@link JwtGrantedAuthoritiesConverter} into
     *       {@code SCOPE_<value>} authorities, e.g. {@code reconciliation:read} becomes
     *       {@code SCOPE_reconciliation:read}. Scopes answer "what may this token do".</li>
     *   <li><b>principal type</b> - the custom {@code principal_type} claim
     *       (USER|MERCHANT|ADMIN|SERVICE) is mapped to a single {@code ROLE_<TYPE>}
     *       authority, e.g. {@code ADMIN} becomes {@code ROLE_ADMIN}, so
     *       {@code hasRole('ADMIN')} works as expected. The role answers "what kind of
     *       caller is this".</li>
     * </ul>
     *
     * <p>Both are needed because a MERCHANT token and an ADMIN token can legitimately
     * carry the same scope, but must not see the same reconciliation rows.</p>
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
        // Name the principal after the identity rather than the raw `sub`, so audit logs
        // and Principal#getName line up with identity_id used elsewhere on the platform.
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
