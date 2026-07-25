package com.paymentprocessor.authenticationservice.controller;

import com.paymentprocessor.authenticationservice.domain.SocialProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only discovery endpoint describing which social login providers are actually
 * available on this deployment.
 *
 * <p>Exists so a login page does not have to hard-code three buttons and hope all three
 * are configured. The SPA calls this on load and renders only the providers it gets back,
 * each with the exact URL to navigate to.
 *
 * <p>Note this controller does <i>not</i> perform any part of the login itself — the flow
 * is a browser redirect to {@code /oauth2/authorization/{provider}}, handled entirely by
 * Spring Security's filters. A fetch/XHR call to that URL will not work, because the
 * provider's consent screen cannot render inside an XHR response; it must be a top-level
 * navigation.
 */
@RestController
@RequestMapping("/api/v1/auth/social")
public class SocialLoginController {

    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;

    /**
     * @param clientRegistrationRepository the configured providers; injected as an
     *                                     {@link ObjectProvider} because the bean is
     *                                     absent entirely when social login is disabled
     */
    public SocialLoginController(ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    /**
     * Describes one available social login provider to the client.
     *
     * @param provider         the provider key, e.g. {@code GOOGLE}
     * @param displayName      human-readable label for the login button, e.g. "Google"
     * @param authorizationUrl the URL the browser must navigate to (top-level, not XHR)
     *                         to begin the login flow
     */
    public record SocialProviderInfo(String provider, String displayName, String authorizationUrl) {}

    /**
     * Lists the social providers configured on this deployment.
     *
     * <p>Public by design (see {@code SecurityConfig}): an unauthenticated user needs this
     * to render the login screen. It exposes no secrets — only which providers exist and
     * the well-known URL to start each flow.
     *
     * @return the enabled providers, or an empty list when social login is not configured
     */
    @GetMapping("/providers")
    public List<SocialProviderInfo> providers() {
        ClientRegistrationRepository repository = clientRegistrationRepository.getIfAvailable();
        if (repository == null) {
            // No provider credentials configured; password login only.
            return List.of();
        }

        List<SocialProviderInfo> result = new ArrayList<>();
        // InMemoryClientRegistrationRepository is the only implementation we publish, and
        // it is the sole variant that can be enumerated - the interface itself offers
        // lookup by id only.
        if (repository instanceof InMemoryClientRegistrationRepository inMemory) {
            for (ClientRegistration registration : inMemory) {
                result.add(new SocialProviderInfo(
                        SocialProvider.fromRegistrationId(registration.getRegistrationId()).name(),
                        registration.getClientName(),
                        "/oauth2/authorization/" + registration.getRegistrationId()));
            }
        }
        return result;
    }
}
