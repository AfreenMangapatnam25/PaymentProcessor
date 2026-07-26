package com.paymentprocessor.authenticationservice.security.oauth2;

import com.paymentprocessor.authenticationservice.domain.SocialProvider;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

/**
 * Provider-agnostic view of "who just logged in", normalised from the wildly
 * inconsistent shapes that Google, GitHub and Microsoft return from their userinfo
 * endpoints.
 *
 * <p>Everything downstream of the OAuth2 handshake
 * ({@link com.paymentprocessor.authenticationservice.service.SocialLoginService})
 * consumes this record rather than a raw {@link OAuth2User}, so adding a fourth
 * provider later means adding one branch in {@link #from} and nothing else.
 *
 * @param provider       which provider authenticated the user
 * @param providerUserId the provider's immutable subject id — never null
 * @param email          the user's email as asserted by the provider, or null if the
 *                       provider did not supply one (common with GitHub unless the
 *                       {@code user:email} scope is granted)
 * @param emailVerified  whether the provider claims to have verified that email.
 *                       Treated conservatively: only true when the provider explicitly
 *                       says so
 * @param displayName    human-readable name, or null
 * @param avatarUrl      profile picture URL, or null
 */
public record SocialUserProfile(
        SocialProvider provider,
        String providerUserId,
        String email,
        boolean emailVerified,
        String displayName,
        String avatarUrl) {

    /**
     * Normalises a provider's userinfo attributes into a {@link SocialUserProfile}.
     *
     * <p>Per-provider attribute differences handled here:
     * <ul>
     *   <li><b>Google</b> (OIDC): {@code sub}, {@code email}, {@code email_verified},
     *       {@code name}, {@code picture}.</li>
     *   <li><b>GitHub</b> (plain OAuth2): {@code id} (numeric), {@code email} (may be
     *       null when the user hides it), {@code name} falling back to {@code login},
     *       {@code avatar_url}. GitHub has no verification flag in the base userinfo
     *       payload, so an email from GitHub is treated as <i>unverified</i>.</li>
     *   <li><b>Microsoft</b> (OIDC / Graph): {@code sub} falling back to {@code oid},
     *       and email arriving as {@code email}, {@code preferred_username} or
     *       {@code userPrincipalName} depending on tenant setup.</li>
     * </ul>
     *
     * @param provider  the provider that produced these attributes
     * @param principal the authenticated OAuth2 principal from Spring Security
     * @return a normalised profile
     * @throws IllegalStateException if the provider did not return a usable subject id,
     *         which would make the login impossible to correlate on subsequent visits
     */
    public static SocialUserProfile from(SocialProvider provider, OAuth2User principal) {
        Map<String, Object> attrs = principal.getAttributes();

        String subject;
        String email;
        boolean verified;
        String name;
        String avatar;

        switch (provider) {
            case GOOGLE -> {
                subject = str(attrs, "sub");
                email = str(attrs, "email");
                verified = bool(attrs, "email_verified");
                name = str(attrs, "name");
                avatar = str(attrs, "picture");
            }
            case GITHUB -> {
                // GitHub's id is a JSON number; str() handles the non-String case.
                subject = str(attrs, "id");
                email = str(attrs, "email");
                // GitHub's userinfo carries no verification flag - assume unverified.
                verified = false;
                name = firstNonBlank(str(attrs, "name"), str(attrs, "login"));
                avatar = str(attrs, "avatar_url");
            }
            case MICROSOFT -> {
                subject = firstNonBlank(str(attrs, "sub"), str(attrs, "oid"));
                email = firstNonBlank(
                        str(attrs, "email"),
                        str(attrs, "preferred_username"),
                        str(attrs, "userPrincipalName"));
                verified = bool(attrs, "email_verified");
                name = firstNonBlank(str(attrs, "name"), str(attrs, "displayName"));
                avatar = null; // Microsoft Graph serves photos from a separate binary endpoint.
            }
            default -> throw new IllegalStateException("Unsupported provider: " + provider);
        }

        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException(
                    "Provider " + provider + " returned no subject id; cannot correlate this login");
        }

        return new SocialUserProfile(
                provider,
                subject,
                email == null || email.isBlank() ? null : email.toLowerCase(),
                verified,
                name,
                avatar);
    }

    /**
     * Reads an attribute as a String, tolerating non-String JSON values (GitHub returns
     * its user id as a number).
     *
     * @param attrs the raw attribute map
     * @param key   the attribute name
     * @return the stringified value, or null if absent
     */
    private static String str(Map<String, Object> attrs, String key) {
        Object value = attrs.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Reads an attribute as a boolean, tolerating both real booleans and the string
     * forms ("true"/"false") that some providers emit.
     *
     * @param attrs the raw attribute map
     * @param key   the attribute name
     * @return the boolean value, defaulting to false when absent or unparseable
     */
    private static boolean bool(Map<String, Object> attrs, String key) {
        Object value = attrs.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * @param values candidate values in preference order
     * @return the first non-null, non-blank value, or null if there is none
     */
    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
