package com.paymentprocessor.limit.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
import org.springframework.util.StringUtils;

/**
 * Security wiring for the Limit Service.
 *
 * <p><b>Where this sits in the platform flow.</b> A human (social or password login) or a machine
 * client authenticates against the <i>authentication-service</i> on port 8081. That service mints
 * an RS256-signed JWT and publishes its public keys at {@code /.well-known/jwks.json}. Every
 * subsequent call into this service carries that token in the {@code Authorization: Bearer ...}
 * header. This class turns the Limit Service into an OAuth2 <b>resource server</b>: it fetches the
 * JWKS, verifies the signature, issuer, expiry and {@code purpose} claim locally, and only then
 * lets the request reach a controller that returns protected limit data. Nothing calls back to the
 * authentication service on the hot path, which is what makes JWKS-based validation scale.
 *
 * <p><b>Why this service in particular.</b> Limits are the platform's spend guardrails: daily,
 * monthly, per-transaction ceilings plus velocity counters, and the reservation endpoints that
 * payment-service calls in-line with authorization. An unauthenticated caller who could raise a
 * limit or release someone else's reservation would defeat the control entirely, so every endpoint
 * beyond probes and documentation requires a token. Typical callers are other services presenting
 * a {@code principal_type=SERVICE} token.
 *
 * <p>The {@code security.jwt.enabled} property selects between the enforcing chain and a permissive
 * development chain; see {@link #jwtSecurityFilterChain} and {@link #permitAllSecurityFilterChain}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Paths that must stay reachable without a token: container/orchestrator probes, the metrics
     * scrape endpoint, the OpenAPI documents and Spring's internal error dispatch. Leaving
     * {@code /error} open matters because a rejected request is forwarded there internally; if it
     * required authentication the client would see a confusing second 401 instead of the real one.
     */
    private static final String[] PUBLIC_PATHS = {
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/prometheus",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error"
    };

    /** Claim carrying the space-delimited OAuth2 scopes granted at login. */
    private static final String SCOPE_CLAIM = "scope";

    /** Claim carrying the kind of identity behind the token: USER, MERCHANT, ADMIN or SERVICE. */
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
     * The enforcing filter chain: every request except {@link #PUBLIC_PATHS} needs a valid JWT.
     *
     * <p>Design notes, i.e. the <i>why</i>:
     * <ul>
     *   <li><b>CSRF disabled</b> &mdash; CSRF defends browser flows that authenticate with an
     *       ambient credential (a cookie). This API authenticates with a bearer token that a
     *       browser never attaches on its own, so a CSRF token would add ceremony and no
     *       protection.</li>
     *   <li><b>STATELESS sessions</b> &mdash; the token <em>is</em> the session. Creating an
     *       {@code HttpSession} would pin a caller to one instance, break horizontal scaling and
     *       silently keep a caller authenticated after their token expired.</li>
     *   <li><b>Method security</b> &mdash; {@code @EnableMethodSecurity} lets controllers layer
     *       {@code @PreAuthorize("hasAuthority('SCOPE_limit:write')")} on top of this coarse gate,
     *       so URL rules stay simple while sensitive operations (raising a limit, force-releasing a
     *       reservation) get precise checks.</li>
     * </ul>
     *
     * @param http                       the chain builder supplied by Spring Security
     * @param jwtAuthenticationConverter converts a verified token into authorities, see
     *                                   {@link #jwtAuthenticationConverter()}
     * @return the configured security filter chain
     * @throws Exception if the chain cannot be built
     */
    @Bean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain jwtSecurityFilterChain(
            HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        log.info("JWT resource-server security ENABLED (issuer={}, jwks={})", issuerUri, jwkSetUri);
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    /**
     * Development-only escape hatch: permits every request without a token.
     *
     * <p><b>Never enable this outside a developer laptop or an ephemeral CI container.</b> It
     * exists so the {@code local} profile &mdash; which loads seeded sample limit configurations
     * and usage counters through Flyway ({@code classpath:db/seed}) &mdash; can be exercised with
     * plain {@code curl} before anyone has stood up the authentication service on port 8081. The
     * toggle lives in configuration rather than in code so the production artifact stays
     * byte-identical to the one developers run.
     *
     * @param http the chain builder supplied by Spring Security
     * @return a permissive filter chain, active only when {@code security.jwt.enabled=false}
     * @throws Exception if the chain cannot be built
     */
    @Bean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "false")
    public SecurityFilterChain permitAllSecurityFilterChain(HttpSecurity http) throws Exception {
        log.warn("JWT validation is DISABLED (security.jwt.enabled=false). "
                + "All endpoints are open - local/dev use only.");
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Translates a cryptographically verified token into the authorities Spring Security checks.
     *
     * <p>Two distinct mappings are produced, and the distinction is intentional:
     * <ul>
     *   <li><b>{@code scope} to {@code SCOPE_*}</b> &mdash; scopes describe <i>what the token is
     *       allowed to do</i>, and are delegated to {@link JwtGrantedAuthoritiesConverter}, which
     *       already knows how to split the space-delimited string. Using the conventional
     *       {@code SCOPE_} prefix means {@code @PreAuthorize("hasAuthority('SCOPE_limit:read')")}
     *       reads the same here as in every other service on the platform.</li>
     *   <li><b>{@code principal_type} to {@code ROLE_*}</b> &mdash; the principal type describes
     *       <i>who the caller is</i> (USER, MERCHANT, ADMIN, SERVICE). Mapping it into the
     *       {@code ROLE_} namespace lets rules use {@code hasRole('ADMIN')}, which Spring expands to
     *       the {@code ROLE_ADMIN} authority. Keeping identity in {@code ROLE_} and delegated
     *       permission in {@code SCOPE_} prevents a broad scope from ever being mistaken for
     *       administrator identity.</li>
     * </ul>
     *
     * @return a converter producing {@code SCOPE_*} authorities plus a single {@code ROLE_*} one
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
            if (StringUtils.hasText(principalType)) {
                authorities.add(new SimpleGrantedAuthority(
                        "ROLE_" + principalType.trim().toUpperCase(Locale.ROOT)));
            }
            return authorities;
        });
        return converter;
    }

    /**
     * Builds the decoder that validates tokens against the authentication service's JWKS.
     *
     * <p>Declaring this bean explicitly (rather than relying on Boot's auto-configuration) buys one
     * thing the defaults do not give: enforcement of the {@code purpose} claim. The authentication
     * service issues several token types from the same key pair, and a refresh or step-up token must
     * never be accepted as an API credential, so anything other than {@code purpose=access} is
     * rejected here alongside the standard expiry/not-before and issuer checks. The signature
     * algorithm is pinned to RS256 so a token cannot downgrade itself through its own header.
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
