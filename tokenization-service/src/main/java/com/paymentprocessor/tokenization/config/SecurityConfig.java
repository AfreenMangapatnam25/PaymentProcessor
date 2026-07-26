package com.paymentprocessor.tokenization.config;

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
 * Stateless OAuth2 <em>resource server</em> security for tokenization-service.
 *
 * <p>How this fits the platform-wide authentication flow:</p>
 * <ol>
 *   <li>A principal authenticates at <strong>authentication-service</strong> (port 8081)
 *       through password or social login; peer services obtain their own tokens the same
 *       way with {@code principal_type=SERVICE}.</li>
 *   <li>authentication-service issues an RS256-signed JWT carrying
 *       {@code sub}/{@code identity_id}, {@code principal_type}, a space-delimited
 *       {@code scope} claim, {@code purpose}, {@code sid} and {@code amr}, and publishes
 *       the matching public keys at {@code /.well-known/jwks.json}.</li>
 *   <li>The caller presents the token here as {@code Authorization: Bearer ...}. This
 *       class configures the chain that fetches the JWKS, verifies the signature and the
 *       pinned issuer, and turns claims into authorities.</li>
 *   <li>Only then do the instrument/card/token endpoints return data.</li>
 * </ol>
 *
 * <p><strong>Sensitivity.</strong> This service is the card vault: it stores PANs,
 * network tokens, BIN ranges and data-encryption-key registry rows, which puts it
 * squarely in PCI-DSS scope. Its endpoints are far more sensitive than the rest of the
 * platform, and in practice it is called <em>service-to-service</em> by payment-service
 * rather than by end users or browsers.</p>
 *
 * <p><strong>Production hardening (recommended next step).</strong> "Any authenticated
 * principal" is deliberately the baseline implemented below so the existing generic CRUD
 * controllers keep working, but it is weaker than this data warrants. In production these
 * endpoints should additionally be restricted to service-principal tokens
 * ({@code principal_type=SERVICE}, i.e. {@code ROLE_SERVICE}), so that a stolen end-user
 * token cannot be replayed against the vault. Two ways to apply it once the callers are
 * confirmed:</p>
 * <pre>{@code
 * // (a) Chain-wide, in jwtSecurityFilterChain below:
 * .requestMatchers("/api/**").hasRole("SERVICE")
 *
 * // (b) Per controller/method, with @EnableMethodSecurity already switched on here:
 * @PreAuthorize("hasRole('SERVICE') and hasAuthority('SCOPE_tokenization:read')")
 * public InstrumentDto get(...) { ... }
 * }</pre>
 * <p>Neither is enabled by default because it would immediately break any human-driven
 * or admin call path that has not yet been migrated to a service token.</p>
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
     * Endpoints intentionally left open: orchestrator health probes and the Prometheus
     * scrape (which run before any identity exists), the OpenAPI documents, and Spring's
     * internal {@code /error} dispatch - protecting {@code /error} would turn every
     * genuine failure into a second, misleading authentication error.
     *
     * <p>Note that no card-bearing path appears here, and none ever should.</p>
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
     * Production/default chain: every request other than {@link #PUBLIC_ENDPOINTS} must
     * carry a valid bearer token.
     *
     * <p>{@code matchIfMissing = true} makes the secure chain the default even if the
     * toggle is missing from a config file - for a card vault, failing closed is the only
     * acceptable behaviour.</p>
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
                // Stateless bearer-token API called by other services: no session cookie
                // exists for a CSRF attack to ride on, and CSRF tokens would break every
                // machine-to-machine caller.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Hardening hint - see the class Javadoc. Once every caller is
                        // known to use a service token, tighten the line below to:
                        //     .requestMatchers("/api/**").hasRole("SERVICE")
                        // so only principal_type=SERVICE tokens can reach the vault.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    /**
     * Development-only chain: permits every request.
     *
     * <p><strong>DEV ONLY - never activate this outside a developer machine, and never
     * against a database holding real card data.</strong> It is selected only by an
     * explicit {@code security.jwt.enabled=false}, which this repo sets solely in the
     * {@code local} profile document of {@code application.yml} so the CRUD controllers
     * and seeded rows can be exercised without running authentication-service.</p>
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
     *       (e.g. {@code tokenization:read} becomes {@code SCOPE_tokenization:read}).
     *       Scopes describe what the token may do.</li>
     *   <li><b>principal type</b> - the custom {@code principal_type} claim
     *       (USER|MERCHANT|ADMIN|SERVICE) becomes one {@code ROLE_<TYPE>} authority, so
     *       {@code principal_type=ADMIN} satisfies {@code hasRole('ADMIN')} and
     *       {@code SERVICE} satisfies {@code hasRole('SERVICE')}. The role describes what
     *       kind of caller is asking.</li>
     * </ul>
     *
     * <p>The role dimension is what makes the service-principal restriction described in
     * the class Javadoc expressible at all - scopes alone cannot distinguish a stolen
     * user token from payment-service's own token.</p>
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
        // Instrument access logs are keyed on the calling identity, so expose identity_id
        // as the principal name instead of the raw `sub`.
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
