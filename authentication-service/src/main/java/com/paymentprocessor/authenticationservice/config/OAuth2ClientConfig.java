package com.paymentprocessor.authenticationservice.config;

import com.paymentprocessor.authenticationservice.domain.SocialProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the set of external identity providers this service can federate to.
 *
 * <h2>Why this is hand-rolled instead of pure {@code spring.security.oauth2.client.*} YAML</h2>
 * Spring Boot's auto-configuration validates every declared registration eagerly: a
 * registration with a blank {@code client-id} aborts application startup. Since these
 * are real third-party credentials that only exist in deployed environments, declaring
 * all three providers in YAML would make the service unstartable on any developer
 * machine that hasn't registered OAuth apps with Google, GitHub <i>and</i> Microsoft.
 *
 * <p>This class instead builds registrations only for the providers whose credentials are
 * actually present. When none are configured it publishes an <b>empty</b> repository rather
 * than no bean at all — see {@link #clientRegistrationRepository(AuthProperties)} for why
 * that distinction matters. {@link SecurityConfig} then omits the social-login filter chain,
 * so the service starts and behaves exactly as before, with password login intact.
 *
 * <h2>Configuration</h2>
 * Credentials are read from {@code auth.social.<provider>.client-id} /
 * {@code .client-secret}, normally injected as environment variables
 * (e.g. {@code GOOGLE_CLIENT_ID}, {@code GOOGLE_CLIENT_SECRET}).
 *
 * <p>The redirect URI registered with each provider must match
 * {@code {baseUrl}/login/oauth2/code/{registrationId}}, e.g.
 * {@code http://localhost:8081/login/oauth2/code/google} for local development.
 */
@Configuration
public class OAuth2ClientConfig {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ClientConfig.class);

    /**
     * The callback path Spring Security listens on for the authorization-code redirect.
     * {@code {baseUrl}} and {@code {registrationId}} are expanded by Spring at runtime.
     */
    private static final String REDIRECT_URI_TEMPLATE = "{baseUrl}/login/oauth2/code/{registrationId}";

    /**
     * Publishes the registry of configured social providers.
     *
     * <p><b>Why this always returns a bean, even with zero providers.</b> Putting
     * {@code spring-boot-starter-oauth2-client} on the classpath makes Spring Security import
     * {@code OAuth2ClientConfiguration}, whose {@code OAuth2AuthorizedClientManager} has a
     * hard, non-optional dependency on a {@link ClientRegistrationRepository}. Returning
     * {@code null} here registers a {@code NullBean}, which does <i>not</i> satisfy that
     * dependency — the context then fails to start with
     * {@code NoSuchBeanDefinitionException: No qualifying bean of type
     * 'ClientRegistrationRepository'}, even though nothing in the application actually wanted
     * social login.
     *
     * <p>So when nothing is configured we return an <b>empty</b> repository: a valid bean that
     * simply resolves no registration ids. Spring is satisfied, and
     * {@link SecurityConfig#hasAnySocialProvider} sees that no provider resolves and skips
     * wiring {@code oauth2Login()} entirely.
     *
     * @param props the bound {@code auth.*} configuration carrying provider credentials
     * @return an {@link InMemoryClientRegistrationRepository} of the configured providers, or
     *         an empty repository when none are configured — never {@code null}
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(AuthProperties props) {
        List<ClientRegistration> registrations = new ArrayList<>();

        AuthProperties.Social social = props.getSocial();
        addIfConfigured(registrations, SocialProvider.GOOGLE, social.getGoogle());
        addIfConfigured(registrations, SocialProvider.GITHUB, social.getGithub());
        addIfConfigured(registrations, SocialProvider.MICROSOFT, social.getMicrosoft());

        if (registrations.isEmpty()) {
            log.info("No social login providers configured (auth.social.*.client-id is blank for "
                    + "google, github and microsoft) - social login is disabled. "
                    + "Password login is unaffected.");
            // Empty-but-valid repository. InMemoryClientRegistrationRepository rejects an
            // empty list, so implement the (functional) interface directly.
            return registrationId -> null;
        }

        log.info("Social login enabled for provider(s): {}",
                registrations.stream().map(ClientRegistration::getRegistrationId).toList());
        return new InMemoryClientRegistrationRepository(registrations);
    }

    /**
     * Appends a provider registration when — and only when — its credentials are present.
     *
     * @param target      the accumulating registration list
     * @param provider    which provider is being configured
     * @param credentials the configured client id/secret pair for that provider
     */
    private void addIfConfigured(List<ClientRegistration> target,
                                 SocialProvider provider,
                                 AuthProperties.ProviderCredentials credentials) {
        if (credentials == null
                || credentials.getClientId() == null || credentials.getClientId().isBlank()
                || credentials.getClientSecret() == null || credentials.getClientSecret().isBlank()) {
            return;
        }
        target.add(build(provider, credentials));
    }

    /**
     * Builds the {@link ClientRegistration} for a single provider.
     *
     * <p>Google and GitHub are assembled from Spring Security's {@link CommonOAuth2Provider}
     * presets, which already carry the correct authorization/token/userinfo endpoints and
     * the attribute name to use as the principal. Microsoft has no preset, so its endpoints
     * are specified explicitly against the {@code common} tenant (which accepts both
     * personal Microsoft accounts and any Azure AD organisational account) — override
     * {@code auth.social.microsoft.tenant} to lock it to a single tenant.
     *
     * @param provider    which provider to build
     * @param credentials the client id/secret (and optional tenant) for that provider
     * @return a fully-formed client registration
     */
    private ClientRegistration build(SocialProvider provider,
                                     AuthProperties.ProviderCredentials credentials) {
        String registrationId = provider.registrationId();

        return switch (provider) {
            case GOOGLE -> CommonOAuth2Provider.GOOGLE
                    .getBuilder(registrationId)
                    .clientId(credentials.getClientId())
                    .clientSecret(credentials.getClientSecret())
                    .redirectUri(REDIRECT_URI_TEMPLATE)
                    // openid+profile+email yields the sub/name/email/email_verified claims
                    // that SocialUserProfile.from() expects.
                    .scope("openid", "profile", "email")
                    .build();

            case GITHUB -> CommonOAuth2Provider.GITHUB
                    .getBuilder(registrationId)
                    .clientId(credentials.getClientId())
                    .clientSecret(credentials.getClientSecret())
                    .redirectUri(REDIRECT_URI_TEMPLATE)
                    // "user:email" is required for the email attribute to be populated at
                    // all; GitHub omits it otherwise. It still arrives unverified, so it
                    // never triggers automatic account linking.
                    .scope("read:user", "user:email")
                    .build();

            case MICROSOFT -> {
                String tenant = credentials.getTenant() == null || credentials.getTenant().isBlank()
                        ? "common"
                        : credentials.getTenant();
                String base = "https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0";
                yield ClientRegistration.withRegistrationId(registrationId)
                        .clientId(credentials.getClientId())
                        .clientSecret(credentials.getClientSecret())
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri(REDIRECT_URI_TEMPLATE)
                        .scope("openid", "profile", "email")
                        .authorizationUri(base + "/authorize")
                        .tokenUri(base + "/token")
                        // Microsoft Graph's /me is used as the userinfo endpoint.
                        .userInfoUri("https://graph.microsoft.com/oidc/userinfo")
                        .userNameAttributeName("sub")
                        .jwkSetUri("https://login.microsoftonline.com/" + tenant + "/discovery/v2.0/keys")
                        .clientName("Microsoft")
                        .build();
            }
        };
    }
}
