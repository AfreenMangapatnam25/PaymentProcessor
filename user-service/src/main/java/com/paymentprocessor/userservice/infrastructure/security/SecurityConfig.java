package com.paymentprocessor.userservice.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless JWT resource-server security.
 *
 * <p><b>Where this sits in the platform flow.</b> A user signs in at
 * <i>authentication-service</i> (:8081) with a password or through a social provider
 * (Google/GitHub/Microsoft). That service mints an RS256-signed JWT and publishes its
 * public keys at {@code /.well-known/jwks.json}. This service verifies each token locally
 * against that JWK set — signature, issuer, expiry and the {@code purpose} claim — before
 * any controller runs.
 *
 * <p>Only health, metrics and API docs are public; everything else requires a valid token.
 * Method-level security ({@code @PreAuthorize}) is enabled for fine-grained scope checks on
 * sensitive operations.
 *
 * <p><b>This service genuinely cannot run without a token.</b> Unlike the other services on
 * the platform, it has no "disable auth for local testing" toggle, and adding one would be
 * misleading: merchant scope and caller identity are resolved server-side from JWT claims
 * ({@code JwtUserContext}), never from the request. With no token there is no merchant
 * context, so the merchant-facing endpoints would fail anyway. Run authentication-service
 * alongside it and let the Postman collection's pre-request script supply the bearer token.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/prometheus",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    /** Claim distinguishing access tokens from refresh/step-up tokens. */
    private static final String PURPOSE_CLAIM = "purpose";

    /** The only {@code purpose} value this API accepts. */
    private static final String PURPOSE_ACCESS = "access";

    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    /** JWKS endpoint of the authentication service; keys are cached and rotated automatically. */
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /** Expected {@code iss} claim. Rejecting foreign issuers stops token-confusion attacks. */
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    /**
     * @param jwtAuthenticationConverter maps verified token claims onto Spring authorities
     */
    public SecurityConfig(JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    /**
     * Builds the resource-server filter chain.
     *
     * @param http the chain builder supplied by Spring Security
     * @return the configured filter chain
     * @throws Exception if the chain cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // stateless API, no cookies/sessions
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    /**
     * Builds the decoder that validates tokens against the authentication service's JWKS.
     *
     * <p><b>Why this bean is declared explicitly.</b> Two reasons, one of them a bug fix:
     * <ol>
     *   <li>Spring Boot's {@code OAuth2ResourceServerJwtConfiguration} publishes a decoder per
     *       configured property — one for {@code jwk-set-uri}, one for {@code issuer-uri}. With
     *       both set it created <i>two</i> {@code JwtDecoder} beans and startup failed with
     *       "required a single bean, but 2 were found". Those auto-configured beans are
     *       {@code @ConditionalOnMissingBean}, so declaring our own suppresses both.</li>
     *   <li>It lets us enforce the {@code purpose} claim. The authentication service issues
     *       refresh tokens and MFA step-up tickets from the same key pair; neither must ever be
     *       accepted as an API credential, so anything other than {@code purpose=access} is
     *       rejected alongside the standard expiry/not-before and issuer checks.</li>
     * </ol>
     * RS256 is pinned so a token cannot downgrade its own algorithm via its header. This
     * mirrors the decoder used by every other service on the platform.
     *
     * @return a {@link NimbusJwtDecoder} that caches and refreshes JWKS keys automatically
     */
    @Bean
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
