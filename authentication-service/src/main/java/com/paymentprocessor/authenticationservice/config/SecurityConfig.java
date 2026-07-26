package com.paymentprocessor.authenticationservice.config;

import com.paymentprocessor.authenticationservice.domain.SocialProvider;
import com.paymentprocessor.authenticationservice.security.JwtAuthenticationFilter;
import com.paymentprocessor.authenticationservice.security.RestAuthenticationEntryPoint;
import com.paymentprocessor.authenticationservice.security.oauth2.OAuth2LoginFailureHandler;
import com.paymentprocessor.authenticationservice.security.oauth2.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * HTTP security for the authentication service.
 *
 * <p>This service plays two distinct OAuth2 roles at once, which is worth keeping clear:
 * <ul>
 *   <li><b>Authorization server (of our own tokens)</b> — it issues the platform's RS256
 *       JWTs and publishes the verification keys at {@code /.well-known/jwks.json}. Every
 *       other microservice is a resource server that validates those tokens.</li>
 *   <li><b>OAuth2 client (towards Google/GitHub/Microsoft)</b> — for social login it acts
 *       as a relying party, sending the user off to an external provider and consuming the
 *       result. See {@link OAuth2ClientConfig}.</li>
 * </ul>
 *
 * <p>The full federated flow:
 * <pre>
 *   Browser  --GET /oauth2/authorization/google-->  this service
 *            &lt;--302 to Google consent screen------
 *            --user grants permission------------&gt;  Google
 *            &lt;--302 to /login/oauth2/code/google--
 *            --code---------------------------- -&gt;  this service
 *                          (exchanges code for provider tokens, loads userinfo)
 *                          (maps external identity -&gt; local Identity)
 *                          (issues OUR RS256 JWT)
 *            &lt;--302 to SPA ?access_token=...------
 *   Browser  --Authorization: Bearer &lt;jwt&gt;------&gt;  any other microservice
 *                          (validates signature against our JWKS -&gt; returns data)
 * </pre>
 *
 * <p>Sessions remain {@link SessionCreationPolicy#STATELESS} for the API surface. The
 * OAuth2 authorization-code handshake itself is completed within a single redirect pair
 * and terminated by {@link OAuth2LoginSuccessHandler}, which hands back a bearer token —
 * no server-side login session is retained afterwards.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;

    /**
     * @param jwtAuthenticationFilter      validates our own bearer tokens on protected routes
     * @param authenticationEntryPoint     renders 401s as JSON instead of a login redirect
     * @param oAuth2LoginSuccessHandler    converts a social login into a platform JWT
     * @param oAuth2LoginFailureHandler    reports consent denials and handshake failures
     * @param clientRegistrationRepository the configured social providers. Injected as an
     *                                     {@link ObjectProvider} because the bean is absent
     *                                     when no provider credentials are configured, in
     *                                     which case social login is simply not wired up
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                          OAuth2LoginFailureHandler oAuth2LoginFailureHandler,
                          ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    /**
     * Builds the single filter chain covering both the token-issuing REST API and the
     * social-login redirect endpoints.
     *
     * @param http the Spring Security builder
     * @return the configured filter chain
     * @throws Exception if the chain cannot be built
     */
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
                // Social login entry point and provider callback. These MUST be public:
                // the user is by definition not yet authenticated when they start the
                // flow, and the provider's redirect back carries no bearer token.
                .requestMatchers(
                        "/oauth2/authorization/**",
                        "/login/oauth2/code/**").permitAll()
                // Lists which social providers are enabled, so a login page can render
                // only the buttons that will actually work.
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/social/providers").permitAll()
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

        // Only enable the social-login filters when at least one provider is configured.
        // Calling oauth2Login() with an empty registry gives a login page that can never
        // succeed, so it is skipped entirely instead.
        if (hasAnySocialProvider(clientRegistrationRepository.getIfAvailable())) {
            http.oauth2Login(oauth -> oauth
                    // GET /oauth2/authorization/{google|github|microsoft} starts the flow.
                    .authorizationEndpoint(a -> a.baseUri("/oauth2/authorization"))
                    // The provider redirects back here with ?code=...&state=...
                    .redirectionEndpoint(r -> r.baseUri("/login/oauth2/code/*"))
                    .successHandler(oAuth2LoginSuccessHandler)
                    .failureHandler(oAuth2LoginFailureHandler));
        }

        return http.build();
    }

    /**
     * Reports whether any social provider is actually usable.
     *
     * <p>The {@link ClientRegistrationRepository} bean is always present (see
     * {@link OAuth2ClientConfig#clientRegistrationRepository}, which returns an empty
     * repository rather than {@code null} so Spring Security's own OAuth2 client plumbing can
     * be satisfied). "Configured" therefore has to be tested by asking the repository whether
     * it can resolve any of the three supported registration ids, rather than by checking the
     * bean for null.
     *
     * @param repository the registry to interrogate; may be {@code null} defensively
     * @return true if at least one of google/github/microsoft resolves to a registration
     */
    private boolean hasAnySocialProvider(ClientRegistrationRepository repository) {
        if (repository == null) {
            return false;
        }
        for (SocialProvider provider : SocialProvider.values()) {
            try {
                if (repository.findByRegistrationId(provider.registrationId()) != null) {
                    return true;
                }
            } catch (RuntimeException e) {
                // Some implementations throw rather than return null for unknown ids.
                // Either way it means "not configured".
            }
        }
        return false;
    }

    /**
     * Password hashing for local (non-federated) credentials.
     *
     * <p>Federated users have no credential row at all, so this only applies to accounts
     * created through password registration.
     *
     * @return an Argon2id encoder with OWASP-aligned parameters (saltLen=16, hashLen=32,
     *         parallelism=1, memory=1&lt;&lt;14 KiB = 16 MiB, iterations=3)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 1 << 14, 3);
    }
}
