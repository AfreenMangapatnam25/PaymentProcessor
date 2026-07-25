package com.paymentprocessor.authenticationservice.domain;

/**
 * The external OAuth2 / OpenID Connect identity providers this service can federate
 * logins from.
 *
 * <p>Each constant corresponds 1:1 to a Spring Security
 * {@code ClientRegistration} whose {@code registrationId} is the lower-cased enum
 * name (e.g. {@code GOOGLE} -&gt; {@code /oauth2/authorization/google}). The mapping
 * is built in
 * {@link com.paymentprocessor.authenticationservice.config.OAuth2ClientConfig}.
 *
 * <p>The provider is stored alongside the provider-assigned user id on
 * {@link com.paymentprocessor.authenticationservice.entity.SocialAccount} so the same
 * person signing in through two different providers results in two link rows, and so
 * a provider that recycles user ids can never collide with another provider's ids.
 */
public enum SocialProvider {

    /** Google (OpenID Connect). Supplies a verified {@code email} and {@code sub}. */
    GOOGLE,

    /**
     * GitHub (plain OAuth2, not OIDC). The primary email is not always present in the
     * userinfo response, so callers may need the {@code user:email} scope for the
     * email to be populated.
     */
    GITHUB,

    /**
     * Microsoft identity platform / Azure AD (OpenID Connect). Email may arrive as
     * {@code email} or {@code preferred_username} depending on tenant configuration.
     */
    MICROSOFT;

    /**
     * Resolves a Spring Security {@code registrationId} (as used in the
     * {@code /oauth2/authorization/{registrationId}} URL) to its enum constant.
     *
     * @param registrationId the registration id, case-insensitive (e.g. "google")
     * @return the matching provider
     * @throws IllegalArgumentException if the id does not correspond to a supported provider
     */
    public static SocialProvider fromRegistrationId(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            throw new IllegalArgumentException("registrationId must not be blank");
        }
        return SocialProvider.valueOf(registrationId.trim().toUpperCase());
    }

    /**
     * @return the Spring Security {@code registrationId} for this provider, i.e. the
     *         lower-cased enum name.
     */
    public String registrationId() {
        return name().toLowerCase();
    }
}
