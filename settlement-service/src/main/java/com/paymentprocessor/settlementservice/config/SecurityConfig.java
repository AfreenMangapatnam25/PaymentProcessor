package com.paymentprocessor.settlementservice.config;

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
 * Stateless OAuth2 <em>resource server</em> security for settlement-service.
 *
 * <p>How this fits the platform-wide authentication flow:</p>
 * <ol>
 *   <li>A merchant operator, back-office admin or peer service authenticates at
 *       <strong>authentication-service</strong> (port 8081) via password or social login.</li>
 *   <li>authentication-service issues an RS256-signed JWT access token carrying
 *       {@code sub}/{@code identity_id}, {@code principal_type}, a space-delimited
 *       {@code scope} claim, {@code purpose}, {@code sid} and {@code amr}, and publishes
 *       its public keys at {@code /.well-known/jwks.json}.</li>
 *   <li>The caller sends it here as {@code Authorization: Bearer ...}; this class builds
 *       the chain that fetches the JWKS, verifies the signature and the pinned issuer,
 *       and maps claims to authorities.</li>
 *   <li>Only then may the settlement APIs read batches or trigger payouts, adjustments
 *       and reversals.</li>
 * </ol>
 *
 * <p>Settlement moves real merchant money, so the API is deny-by-default. This service
 * verifies tokens only; it never issues them.</p>
 *
 * <p><b>Profiles:</b> unlike most services here, settlement-service uses {@code dev}
 * (H2, active by default) and {@code prod} - there is no {@code local} profile. The
 * {@code dev} document sets {@code security.jwt.enabled=false}; {@code prod} leaves the
 * default of {@code true} and therefore requires a valid token.</p>
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
     * Endpoints intentionally reachable without a token: orchestrator health/readiness
     * probes and the Prometheus scrape (both run before any identity exists), the OpenAPI
     * documents, and Spring's internal {@code /error} dispatch - if {@code /error} were
     * itself protected, every genuine error would be reported as an authentication
     * failure instead.
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
     * Production/default chain (active under the {@code prod} profile, and anywhere the
     * toggle is absent): everything except {@link #PUBLIC_ENDPOINTS} requires a valid
     * bearer token.
     *
     * <p>{@code matchIfMissing = true} means a config file that forgets the property
     * still gets the secure chain - this fails closed, which is the only acceptable
     * default for a money-movement API.</p>
     *
     * <p>Note that {@code /h2-console/**} is <em>not</em> public here: the H2 console is
     * only enabled under the {@code dev} profile, and exposing it in production would
     * hand out raw database access.</p>
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
                // Stateless bearer-token API: no session cookie exists for a CSRF attack to
                // ride on, and CSRF tokens would break every server-to-server caller.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Deny by default so any settlement endpoint added later is
                        // protected without anyone having to remember to list it.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    /**
     * Development-only chain: permits every request, including the H2 console.
     *
     * <p><strong>DEV ONLY - must never be active in a shared or production
     * environment.</strong> It is selected only by an explicit
     * {@code security.jwt.enabled=false}, which is set exclusively in the {@code dev}
     * profile document of {@code application.yml} so the in-memory H2 database and seeded
     * settlement batches can be exercised without running authentication-service.</p>
     *
     * <p><b>CSRF and frame options.</b> The H2 web console is a plain HTML form app that
     * renders itself inside frames, so two of Spring Security's browser defaults have to
     * be relaxed for it:</p>
     * <ul>
     *   <li>CSRF is disabled - the console posts SQL forms without a Spring CSRF token,
     *       so every statement would otherwise be rejected with 403. It is already
     *       disabled chain-wide because this is a stateless API.</li>
     *   <li>{@code X-Frame-Options} defaults to {@code DENY}, which leaves the console's
     *       frameset blank; {@code sameOrigin()} allows framing from this host only. That
     *       is a clickjacking relaxation and is another reason this chain is dev-only.</li>
     * </ul>
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
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        // H2 console: dev profile only (spring.h2.console.enabled=true there).
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().permitAll());
        return http.build();
    }

    /**
     * Translates authentication-service's claims into the authorities read by
     * {@code @PreAuthorize} and {@code hasAuthority(...)}.
     *
     * <ul>
     *   <li><b>scopes</b> - the space-delimited {@code scope} claim is split by
     *       {@link JwtGrantedAuthoritiesConverter} into {@code SCOPE_<value>} authorities
     *       (e.g. {@code settlement:write} becomes {@code SCOPE_settlement:write}). Scopes
     *       say what the token may do.</li>
     *   <li><b>principal type</b> - the custom {@code principal_type} claim
     *       (USER|MERCHANT|ADMIN|SERVICE) becomes one {@code ROLE_<TYPE>} authority, so
     *       {@code principal_type=ADMIN} satisfies {@code hasRole('ADMIN')}. The role says
     *       what kind of caller is asking.</li>
     * </ul>
     *
     * <p>Both are needed: settlement adjustments above the configured thresholds are
     * meant for supervisors/finance directors, which is a statement about the caller's
     * type, not merely about the scope on the token.</p>
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
        // Payout approval trails are keyed on identity_id, so surface that as the
        // principal name rather than the raw `sub`.
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
