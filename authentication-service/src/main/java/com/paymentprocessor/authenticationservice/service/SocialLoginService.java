package com.paymentprocessor.authenticationservice.service;

import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.domain.IdentityStatus;
import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import com.paymentprocessor.authenticationservice.dto.TokenResponse;
import com.paymentprocessor.authenticationservice.entity.Identity;
import com.paymentprocessor.authenticationservice.entity.SocialAccount;
import com.paymentprocessor.authenticationservice.repository.IdentityRepository;
import com.paymentprocessor.authenticationservice.repository.SocialAccountRepository;
import com.paymentprocessor.authenticationservice.security.JwtService;
import com.paymentprocessor.authenticationservice.security.oauth2.SocialUserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns a provider-verified social login into a local identity and a first-party
 * RS256 access token.
 *
 * <p>This is the hinge of the federated-login flow. Everything before it is standard
 * Spring Security OAuth2 client machinery (redirect to provider, user consents,
 * authorization code exchanged for provider tokens, userinfo fetched). Everything after
 * it is the platform's existing, unchanged JWT world — the token this class returns is
 * byte-for-byte the same shape as one issued by password login, signed with the same
 * key, verifiable against the same {@code /.well-known/jwks.json}. Downstream services
 * therefore need no knowledge that social login exists.
 *
 * <h2>Account resolution strategy</h2>
 * On each login, in order:
 * <ol>
 *   <li><b>Known external account</b> — a {@link SocialAccount} row exists for
 *       (provider, subject). Reuse its identity. This is the steady-state path.</li>
 *   <li><b>Account linking</b> — no link row, but the provider asserts a
 *       <i>verified</i> email matching an existing local identity. Link the provider to
 *       that identity so the user isn't given a second, duplicate account.</li>
 *   <li><b>Provisioning</b> — neither of the above. Create a new {@link Identity} plus
 *       its link row (JIT provisioning).</li>
 * </ol>
 *
 * <h2>Security note on step 2</h2>
 * Linking by email is only done when the provider explicitly marks the email verified.
 * Auto-linking on an <i>unverified</i> email is a well-known account-takeover vector:
 * an attacker registers {@code victim@example.com} at a provider that never verifies
 * it, signs in here, and is handed the victim's existing account. GitHub does not
 * expose a verification flag in its base userinfo payload, so GitHub logins never
 * auto-link — they provision a fresh identity instead, and the user can link accounts
 * explicitly later from an authenticated session.
 */
@Service
public class SocialLoginService {

    private static final Logger log = LoggerFactory.getLogger(SocialLoginService.class);

    private final SocialAccountRepository socialAccountRepository;
    private final IdentityRepository identityRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final ScopeResolver scopeResolver;
    private final long accessTtlSeconds;

    /**
     * @param socialAccountRepository store of provider-to-identity link rows
     * @param identityRepository      store of local identities
     * @param refreshTokenService     issues the opaque, rotating refresh token
     * @param jwtService              signs the RS256 access token
     * @param scopeResolver           maps principal type to default token scopes
     * @param props                   supplies the access-token TTL advertised to clients
     */
    public SocialLoginService(SocialAccountRepository socialAccountRepository,
                              IdentityRepository identityRepository,
                              RefreshTokenService refreshTokenService,
                              JwtService jwtService,
                              ScopeResolver scopeResolver,
                              AuthProperties props) {
        this.socialAccountRepository = socialAccountRepository;
        this.identityRepository = identityRepository;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.scopeResolver = scopeResolver;
        this.accessTtlSeconds = props.getJwt().getAccessTokenTtl().toSeconds();
    }

    /**
     * Completes a social login: resolves (or creates) the local identity behind the
     * provider profile, then issues this platform's own access + refresh tokens.
     *
     * <p>Runs in a single transaction so that identity creation and link-row creation
     * either both commit or both roll back — a half-provisioned user with no link row
     * would silently create a duplicate account on their next login.
     *
     * @param profile the normalised, provider-verified user profile
     * @return a bearer token response identical in shape to password-login responses
     * @throws SocialLoginDeniedException if the resolved identity is locked or disabled
     */
    @Transactional
    public TokenResponse completeLogin(SocialUserProfile profile) {
        Identity identity = resolveIdentity(profile);

        // A federated login must not become a way around account suspension.
        if (identity.getStatus() == IdentityStatus.LOCKED
                || identity.getStatus() == IdentityStatus.DISABLED) {
            throw new SocialLoginDeniedException(
                    "Identity " + identity.getId() + " is " + identity.getStatus());
        }

        // A successful federated login proves control of the external account, which we
        // treat as sufficient to clear a PENDING (unverified) identity - provided the
        // provider vouched for the email.
        if (identity.getStatus() == IdentityStatus.PENDING && profile.emailVerified()) {
            identity.setStatus(IdentityStatus.ACTIVE);
            if (identity.getEmailVerifiedAt() == null) {
                identity.setEmailVerifiedAt(Instant.now());
            }
            identityRepository.save(identity);
        }

        return issueTokens(identity, profile);
    }

    /**
     * Applies the three-step resolution strategy documented on this class: known link,
     * then verified-email link, then just-in-time provisioning.
     *
     * @param profile the normalised provider profile
     * @return the local identity this social login maps to
     */
    private Identity resolveIdentity(SocialUserProfile profile) {
        // Step 1 - returning user: this external account has logged in before.
        Optional<SocialAccount> existingLink = socialAccountRepository
                .findByProviderAndProviderUserId(profile.provider(), profile.providerUserId());

        if (existingLink.isPresent()) {
            SocialAccount link = existingLink.get();
            refreshLinkMetadata(link, profile);
            return identityRepository.findById(link.getIdentityId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Dangling social_accounts row " + link.getId()
                                    + " references missing identity " + link.getIdentityId()));
        }

        // Step 2 - existing local user adopting a new provider. Only safe on a
        // provider-verified email (see the class-level security note).
        if (profile.email() != null && profile.emailVerified()) {
            Optional<Identity> byEmail = identityRepository.findByEmailIgnoreCase(profile.email());
            if (byEmail.isPresent()) {
                Identity identity = byEmail.get();
                createLink(identity.getId(), profile);
                log.info("Linked {} account to existing identity {} via verified email",
                        profile.provider(), identity.getId());
                return identity;
            }
        }

        // Step 3 - brand-new user: provision an identity just in time.
        Identity provisioned = provisionIdentity(profile);
        createLink(provisioned.getId(), profile);
        log.info("Provisioned new identity {} from {} login", provisioned.getId(), profile.provider());
        return provisioned;
    }

    /**
     * Creates a local identity for a first-time social user.
     *
     * <p>The identity is created with no credential row — these users have no password
     * here, and authenticate solely through the provider. Status is ACTIVE when the
     * provider verified the email and PENDING otherwise, so an unverified federated
     * user still has to complete this platform's own email verification before being
     * treated as fully established.
     *
     * @param profile the normalised provider profile
     * @return the newly persisted identity
     */
    private Identity provisionIdentity(SocialUserProfile profile) {
        Identity identity = new Identity();
        identity.setId(UUID.randomUUID().toString());
        // Federated sign-ups are end users; staff/service principals are created by admins.
        identity.setPrincipalType(PrincipalType.USER);
        identity.setEmail(profile.email());
        identity.setStatus(profile.emailVerified() ? IdentityStatus.ACTIVE : IdentityStatus.PENDING);
        if (profile.emailVerified()) {
            identity.setEmailVerifiedAt(Instant.now());
        }
        // Social logins do not force a second factor by default; a user can still enrol
        // TOTP afterwards through the existing MFA endpoints.
        identity.setMfaRequired(false);
        return identityRepository.save(identity);
    }

    /**
     * Persists a new provider-to-identity link row.
     *
     * @param identityId the local identity to link to
     * @param profile    the normalised provider profile supplying the link metadata
     */
    private void createLink(String identityId, SocialUserProfile profile) {
        SocialAccount link = new SocialAccount();
        link.setId(UUID.randomUUID().toString());
        link.setIdentityId(identityId);
        link.setProvider(profile.provider());
        link.setProviderUserId(profile.providerUserId());
        link.setEmail(profile.email());
        link.setDisplayName(profile.displayName());
        link.setAvatarUrl(profile.avatarUrl());
        socialAccountRepository.save(link);
    }

    /**
     * Refreshes the cached profile fields on an existing link row so the local copy of
     * the user's name/avatar/email does not drift from the provider over time, and
     * stamps the login time.
     *
     * @param link    the existing link row
     * @param profile the freshly fetched provider profile
     */
    private void refreshLinkMetadata(SocialAccount link, SocialUserProfile profile) {
        link.setEmail(profile.email());
        link.setDisplayName(profile.displayName());
        link.setAvatarUrl(profile.avatarUrl());
        link.setLastLoginAt(Instant.now());
        socialAccountRepository.save(link);
    }

    /**
     * Mints the platform's own tokens for a resolved identity.
     *
     * <p>Mirrors {@code AuthenticationService.issueSession} so that social and password
     * logins are indistinguishable to every downstream consumer. The {@code amr}
     * (authentication methods reference) claim records how the user proved themselves,
     * so a resource server can require step-up auth for sensitive operations if it ever
     * needs to distinguish federated from password logins.
     *
     * @param identity the authenticated local identity
     * @param profile  the provider profile, used only for the {@code amr} claim
     * @return the bearer token response
     */
    private TokenResponse issueTokens(Identity identity, SocialUserProfile profile) {
        RefreshTokenService.Issued issued =
                refreshTokenService.issue(identity.getId(), null, null);

        List<String> scopes = scopeResolver.defaultScopes(identity.getPrincipalType());

        // e.g. ["federated", "google"] - marks this as an external-IdP login.
        List<String> amr = List.of("federated", profile.provider().registrationId());

        JwtService.IssuedToken access = jwtService.issueAccessToken(
                identity.getId(),
                identity.getPrincipalType(),
                scopes,
                issued.familyId(),
                amr);

        return TokenResponse.bearer(
                access.value(), accessTtlSeconds, issued.rawToken(), issued.familyId());
    }

    /**
     * Raised when the provider authenticated the user successfully but this platform
     * refuses the session anyway — currently only for locked or disabled identities.
     * Surfaces as a {@code 403}-style redirect by the OAuth2 failure handler.
     */
    public static class SocialLoginDeniedException extends RuntimeException {
        /**
         * @param message operator-facing reason the login was denied
         */
        public SocialLoginDeniedException(String message) {
            super(message);
        }
    }
}
