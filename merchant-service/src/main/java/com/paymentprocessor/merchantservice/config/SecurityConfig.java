package com.paymentprocessor.merchantservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.merchantservice.common.error.ApiError;
import com.paymentprocessor.merchantservice.security.ApiKeyAuthFilter;
import com.paymentprocessor.merchantservice.security.ApiKeyAuthenticator;
import com.paymentprocessor.merchantservice.security.SecurityRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Security wiring for the Merchant Service.
 *
 * <p><b>Where this sits in the platform flow.</b> A user signs in at the
 * <i>authentication-service</i> (port 8081) with a password or through a social provider
 * (Google/GitHub/Microsoft). That service mints an RS256-signed JWT and publishes its public keys
 * at {@code /.well-known/jwks.json}. This service validates those tokens locally against that JWK
 * set — signature, issuer, expiry and the {@code purpose} claim — before any controller runs.
 *
 * <p><b>Two credential types, one header.</b> This service is unusual on the platform in accepting
 * both:
 * <ul>
 *   <li><b>API keys</b> — long-lived, opaque, issued per merchant by this service itself
 *       ({@code /api/v1/merchants/{id}/api-keys}). Used by merchant server-side integrations that
 *       have no interactive user to log in. Handled by {@link ApiKeyAuthFilter}, accepted on
 *       {@code Authorization: Bearer <key>} or {@code X-API-Key}.</li>
 *   <li><b>JWT access tokens</b> — short-lived, issued by the authentication service, representing
 *       a human or a service principal. Handled by Spring Security's resource-server filter.</li>
 * </ul>
 * Because both arrive on {@code Authorization: Bearer}, {@link ApiKeyAuthFilter} defers anything
 * JWT-shaped (three dot-separated segments) to the resource server, and API keys — opaque random
 * strings with no dots — are unaffected. Either credential can satisfy a request; they are
 * alternatives, not a required pair.
 *
 * <p><b>Authority model.</b> The pre-existing URL rules are written against the role names in
 * {@link SecurityRoles} ({@code ADMIN}, {@code READ}, {@code WRITE}), which API keys supply
 * directly. For JWTs to work against those same rules, {@link #jwtAuthenticationConverter()} maps
 * token claims onto that vocabulary as well as the platform-standard {@code SCOPE_*} authorities —
 * see that method for the mapping and its rationale.
 *
 * <p><b>Why two filter chains.</b> Exactly one of the two {@link SecurityFilterChain} beans below
 * is active, selected by {@code security.jwt.enabled}. The enforcing chain is the default. The
 * API-key-only chain runs when the toggle is off, so the {@code local} profile — which loads a
 * seeded sample merchant through Flyway — can be exercised without standing up the authentication
 * service first.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /** Claim carrying the space-delimited OAuth2 scopes granted to the token. */
    private static final String SCOPE_CLAIM = "scope";

    /** Claim carrying the caller's principal type: USER, MERCHANT, ADMIN or SERVICE. */
    private static final String PRINCIPAL_TYPE_CLAIM = "principal_type";

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
     * Registers the API-key authentication filter.
     *
     * @param authenticator resolves and validates a presented API key
     * @param objectMapper  serialises the structured {@link ApiError} body on failure
     * @return the filter, wired into both chains below
     */
    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter(ApiKeyAuthenticator authenticator, ObjectMapper objectMapper) {
        return new ApiKeyAuthFilter(authenticator, objectMapper);
    }

    /**
     * The enforcing chain: accepts either a valid API key or a valid JWT.
     *
     * <p>Authorization rules are unchanged from the API-key-only design — onboarding, lifecycle
     * transitions, compliance decisions and pricing require {@code ADMIN}; other reads require
     * {@code READ} or {@code ADMIN}; other writes require {@code WRITE} or {@code ADMIN}.
     *
     * <p>Design notes, i.e. the <i>why</i>:
     * <ul>
     *   <li><b>CSRF disabled</b> — CSRF defends browser flows authenticating with an ambient
     *       cookie. Both credential types here are explicit headers a browser never attaches on
     *       its own, so a CSRF token would add ceremony and no protection.</li>
     *   <li><b>STATELESS sessions</b> — the credential <em>is</em> the session. An
     *       {@code HttpSession} would pin callers to one instance and break horizontal
     *       scaling.</li>
     *   <li><b>API-key filter before the resource server</b> — it runs first but only claims
     *       non-JWT credentials, and it no-ops when the context is already authenticated, so
     *       order is safe either way.</li>
     * </ul>
     *
     * @param http                       the chain builder supplied by Spring Security
     * @param apiKeyAuthFilter           the API-key filter
     * @param objectMapper               serialises error bodies
     * @param jwtAuthenticationConverter converts a verified token into authorities
     * @return the configured security filter chain
     * @throws Exception if the chain cannot be built
     */
    @Bean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain filterChain(HttpSecurity http, ApiKeyAuthFilter apiKeyAuthFilter,
                                           ObjectMapper objectMapper,
                                           JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        log.info("Merchant security ENABLED: API key or JWT accepted (issuer={}, jwks={})",
                issuerUri, jwkSetUri);
        applyCommonRules(http, apiKeyAuthFilter, objectMapper)
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    /**
     * Reduced chain used when {@code security.jwt.enabled=false}: API keys still work, JWT
     * validation is simply not wired.
     *
     * <p>Unlike most services on the platform this is <b>not</b> a permit-all chain — the
     * pre-existing API-key gate is left fully in force, because it does not depend on the
     * authentication service being reachable. The only thing the toggle removes is the JWT path.
     * This keeps the {@code local} profile usable (API key {@code admin_local_dev_key_change_me}
     * ships in the seeded config) without opening the service up entirely.
     *
     * @param http             the chain builder supplied by Spring Security
     * @param apiKeyAuthFilter the API-key filter
     * @param objectMapper     serialises error bodies
     * @return an API-key-only filter chain
     * @throws Exception if the chain cannot be built
     */
    @Bean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "false")
    public SecurityFilterChain apiKeyOnlyFilterChain(HttpSecurity http, ApiKeyAuthFilter apiKeyAuthFilter,
                                                     ObjectMapper objectMapper) throws Exception {
        log.warn("JWT validation is DISABLED (security.jwt.enabled=false); API-key authentication "
                + "remains enforced. Local/dev use only.");
        applyCommonRules(http, apiKeyAuthFilter, objectMapper);
        return http.build();
    }

    /**
     * Applies the request-authorization rules, session policy and error handling shared by both
     * chains, so the two cannot drift apart.
     *
     * @param http             the chain builder
     * @param apiKeyAuthFilter the API-key filter to insert
     * @param objectMapper     serialises error bodies
     * @return the same builder, for fluent chaining by the caller
     * @throws Exception if configuration fails
     */
    private HttpSecurity applyCommonRules(HttpSecurity http, ApiKeyAuthFilter apiKeyAuthFilter,
                                          ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public infrastructure endpoints
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
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
    }

    /**
     * Translates a cryptographically verified token into the authorities this service's rules check.
     *
     * <p>Three mappings are produced. The first two are the platform standard; the third exists
     * specifically so JWT callers can satisfy the pre-existing API-key-era URL rules:
     * <ul>
     *   <li><b>{@code scope} to {@code SCOPE_*}</b> — what the token is <i>allowed to do</i>,
     *       delegated to {@link JwtGrantedAuthoritiesConverter}. Consistent with every other
     *       service, so {@code @PreAuthorize("hasAuthority('SCOPE_merchant:read')")} reads the
     *       same everywhere.</li>
     *   <li><b>{@code principal_type} to {@code ROLE_*}</b> — who the caller <i>is</i> (USER,
     *       MERCHANT, ADMIN, SERVICE). Keeping identity in {@code ROLE_} and delegated permission
     *       in {@code SCOPE_} prevents a broad scope from being mistaken for administrator
     *       identity.</li>
     *   <li><b>Derived {@code ROLE_READ} / {@code ROLE_WRITE}</b> — the URL rules above predate
     *       JWT support and are expressed in the API key's coarse READ/WRITE vocabulary. Rather
     *       than rewrite every rule, tokens are granted {@code ROLE_READ} always (any
     *       authenticated principal may read) and {@code ROLE_WRITE} when the token carries a
     *       write-ish scope or an elevated principal type. This is deliberately conservative:
     *       a plain {@code user:self} token can read but not mutate merchant records.</li>
     * </ul>
     *
     * @return a converter producing {@code SCOPE_*} plus the {@code ROLE_*} authorities the
     *         request rules are written against
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        scopeConverter.setAuthoritiesClaimName(SCOPE_CLAIM);
        scopeConverter.setAuthorityPrefix("SCOPE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        // "sub" and "identity_id" carry the same value; "sub" is standard and always present.
        converter.setPrincipalClaimName("sub");
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopeConverter.convert(jwt));

            String principalType = jwt.getClaimAsString(PRINCIPAL_TYPE_CLAIM);
            String normalisedType = principalType == null
                    ? "" : principalType.trim().toUpperCase(Locale.ROOT);
            if (!normalisedType.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + normalisedType));
            }

            // Any authenticated principal may read.
            authorities.add(new SimpleGrantedAuthority("ROLE_" + SecurityRoles.READ));

            // Mutation requires either an elevated principal type or an explicit write scope.
            String scope = jwt.getClaimAsString(SCOPE_CLAIM);
            List<String> scopes = scope == null || scope.isBlank()
                    ? List.of()
                    : List.of(scope.trim().split("\\s+"));
            boolean writeCapable = normalisedType.equals("ADMIN")
                    || normalisedType.equals("MERCHANT")
                    || normalisedType.equals("SERVICE")
                    || scopes.stream().anyMatch(s ->
                            s.equals("admin")
                            || s.endsWith(":write")
                            || s.endsWith(":admin")
                            || s.equals("merchant:self"));
            if (writeCapable) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + SecurityRoles.WRITE));
            }

            return authorities;
        });
        return converter;
    }

    /**
     * Builds the decoder that validates tokens against the authentication service's JWKS.
     *
     * <p>Declared explicitly rather than left to Boot's auto-configuration so the {@code purpose}
     * claim can be enforced: the authentication service issues refresh tokens and MFA step-up
     * tickets from the same key pair, and neither must ever be accepted as an API credential.
     * Anything other than {@code purpose=access} is rejected alongside the standard
     * expiry/not-before and issuer checks. RS256 is pinned so a token cannot downgrade its own
     * algorithm through its header.
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

    /**
     * Writes a structured {@link ApiError} JSON body for authentication/authorization failures,
     * so clients get a consistent error shape rather than an empty 401/403.
     *
     * @param objectMapper serialiser
     * @param response     the servlet response
     * @param path         the request URI, echoed in the error body
     * @param status       the HTTP status to return
     * @param code         a stable machine-readable error code
     * @param message      a human-readable explanation
     * @throws java.io.IOException if the body cannot be written
     */
    private void writeError(ObjectMapper objectMapper, jakarta.servlet.http.HttpServletResponse response,
                            String path, HttpStatus status, String code, String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(status.value(), status.getReasonPhrase(), code, message, path);
        objectMapper.writeValue(response.getWriter(), error);
    }
}
