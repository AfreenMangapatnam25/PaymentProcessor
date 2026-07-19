package com.paymentprocessor.authenticationservice.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.domain.IdentityStatus;
import com.paymentprocessor.authenticationservice.domain.LoginResult;
import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import com.paymentprocessor.authenticationservice.dto.*;
import com.paymentprocessor.authenticationservice.entity.Identity;
import com.paymentprocessor.authenticationservice.event.AuthEvents;
import com.paymentprocessor.authenticationservice.event.DomainEventPublisher;
import com.paymentprocessor.authenticationservice.exception.AccountLockedException;
import com.paymentprocessor.authenticationservice.exception.ForbiddenException;
import com.paymentprocessor.authenticationservice.exception.UnauthorizedException;
import com.paymentprocessor.authenticationservice.repository.IdentityRepository;
import com.paymentprocessor.authenticationservice.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Orchestrates the end-to-end authentication flows described in the service README. */
@Service
public class AuthenticationService {

    public record RequestContext(String ip, String userAgent) {}

    private final IdentityRepository identityRepository;
    private final CredentialService credentialService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final MfaService mfaService;
    private final DeviceService deviceService;
    private final LoginAttemptService loginAttemptService;
    private final RateLimiterService rateLimiter;
    private final ScopeResolver scopeResolver;
    private final DomainEventPublisher events;
    private final AuthProperties.Lockout lockout;
    private final long accessTtlSeconds;

    public AuthenticationService(IdentityRepository identityRepository,
                                 CredentialService credentialService,
                                 RefreshTokenService refreshTokenService,
                                 JwtService jwtService,
                                 MfaService mfaService,
                                 DeviceService deviceService,
                                 LoginAttemptService loginAttemptService,
                                 RateLimiterService rateLimiter,
                                 ScopeResolver scopeResolver,
                                 DomainEventPublisher events,
                                 AuthProperties props) {
        this.identityRepository = identityRepository;
        this.credentialService = credentialService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.mfaService = mfaService;
        this.deviceService = deviceService;
        this.loginAttemptService = loginAttemptService;
        this.rateLimiter = rateLimiter;
        this.scopeResolver = scopeResolver;
        this.events = events;
        this.lockout = props.getLockout();
        this.accessTtlSeconds = props.getJwt().getAccessTokenTtl().toSeconds();
    }

    // ---------------------------------------------------------------- login

    @Transactional
    public LoginResponse login(LoginRequest req, RequestContext ctx) {
        rateLimiter.checkLogin(rateKey(req.email(), ctx));

        Identity identity = identityRepository.findByEmailIgnoreCase(req.email()).orElse(null);
        if (identity == null) {
            loginAttemptService.record(null, req.email(), ctx.ip(), ctx.userAgent(), LoginResult.BAD_CREDENTIALS);
            throw new UnauthorizedException("Invalid credentials");
        }

        assertLoginable(identity, ctx);

        if (!credentialService.verifyPassword(identity.getId(), req.password())) {
            handleFailedPassword(identity, ctx);
            throw new UnauthorizedException("Invalid credentials");
        }

        // Successful password: reset failure counter.
        if (identity.getFailedLoginCount() != 0) {
            identity.setFailedLoginCount(0);
            identityRepository.save(identity);
        }

        String deviceId = touchDevice(identity, req.deviceFingerprint(), req.deviceLabel(), ctx);

        if (identity.isMfaRequired() || mfaService.hasActiveMfa(identity.getId())) {
            loginAttemptService.record(identity.getId(), req.email(), ctx.ip(), ctx.userAgent(),
                    LoginResult.MFA_REQUIRED);
            JwtService.IssuedToken ticket =
                    jwtService.issueMfaTicket(identity.getId(), identity.getPrincipalType());
            List<String> methods = mfaService.activeMethods(identity.getId());
            return LoginResponse.mfaRequired(new MfaChallenge(ticket.value(), methods));
        }

        TokenResponse tokens = issueSession(identity, deviceId, List.of("pwd"));
        loginAttemptService.record(identity.getId(), req.email(), ctx.ip(), ctx.userAgent(), LoginResult.SUCCESS);
        events.publish(AuthEvents.UserLoggedIn.of(identity.getId(),
                identity.getPrincipalType().name(), deviceId, ctx.ip()));
        return LoginResponse.authenticated(tokens);
    }

    @Transactional
    public TokenResponse completeMfaLogin(MfaLoginRequest req, RequestContext ctx) {
        JWTClaimsSet claims;
        try {
            claims = jwtService.verify(req.mfaToken());
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid or expired MFA session");
        }
        if (!JwtService.PURPOSE_MFA.equals(jwtService.purpose(claims))) {
            throw new UnauthorizedException("Invalid MFA session");
        }
        String identityId = claims.getSubject();
        Identity identity = identityRepository.findById(identityId)
                .orElseThrow(() -> new UnauthorizedException("Invalid MFA session"));
        assertLoginable(identity, ctx);

        if (!mfaService.verifyChallenge(identityId, req.code())) {
            loginAttemptService.record(identityId, identity.getEmail(), ctx.ip(), ctx.userAgent(),
                    LoginResult.MFA_FAILED);
            throw new UnauthorizedException("Invalid MFA code");
        }

        String deviceId = touchDevice(identity, req.deviceFingerprint(), req.deviceLabel(), ctx);
        TokenResponse tokens = issueSession(identity, deviceId, List.of("pwd", "mfa"));
        loginAttemptService.record(identityId, identity.getEmail(), ctx.ip(), ctx.userAgent(), LoginResult.SUCCESS);
        events.publish(AuthEvents.UserLoggedIn.of(identityId, identity.getPrincipalType().name(), deviceId, ctx.ip()));
        return tokens;
    }

    // -------------------------------------------------------------- refresh

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken);
        Identity identity = identityRepository.findById(rotation.identityId())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (identity.getStatus() != IdentityStatus.ACTIVE) {
            refreshTokenService.revokeAllForIdentity(identity.getId());
            throw new UnauthorizedException("Account is not active");
        }
        List<String> scopes = scopeResolver.defaultScopes(identity.getPrincipalType());
        JwtService.IssuedToken access = jwtService.issueAccessToken(
                identity.getId(), identity.getPrincipalType(), scopes, rotation.familyId(), List.of("pwd"));
        return TokenResponse.bearer(access.value(), accessTtlSeconds, rotation.rawToken(), rotation.familyId());
    }

    // --------------------------------------------------------------- logout

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken).ifPresent(info ->
                events.publish(AuthEvents.UserLoggedOut.of(info.identityId(), info.familyId(), false)));
    }

    @Transactional
    public void logoutAll(String identityId) {
        refreshTokenService.revokeAllForIdentity(identityId);
        events.publish(AuthEvents.UserLoggedOut.of(identityId, null, true));
    }

    // ----------------------------------------------------------- introspect

    @Transactional(readOnly = true)
    public IntrospectResponse introspect(String token) {
        try {
            JWTClaimsSet claims = jwtService.verify(token);
            if (!JwtService.PURPOSE_ACCESS.equals(jwtService.purpose(claims))) {
                return IntrospectResponse.inactive();
            }
            List<String> scopes = List.of();
            Object scope = claims.getClaim("scope");
            if (scope != null && !scope.toString().isBlank()) {
                scopes = List.of(scope.toString().split(" "));
            }
            long exp = claims.getExpirationTime().toInstant().getEpochSecond();
            return new IntrospectResponse(true, claims.getSubject(),
                    String.valueOf(claims.getClaim("principal_type")), scopes, exp);
        } catch (Exception e) {
            return IntrospectResponse.inactive();
        }
    }

    // ----------------------------------------------------- change password

    @Transactional
    public void changePassword(String identityId, String currentPassword, String newPassword) {
        if (!credentialService.verifyPassword(identityId, currentPassword)) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        credentialService.setPassword(identityId, newPassword);
        refreshTokenService.revokeAllForIdentity(identityId);
        events.publish(AuthEvents.PasswordChanged.of(identityId, false));
    }

    // --------------------------------------------------------------- helpers

    private void assertLoginable(Identity identity, RequestContext ctx) {
        if (identity.getStatus() == IdentityStatus.DISABLED) {
            throw new ForbiddenException("Account is disabled");
        }
        if (identity.getStatus() == IdentityStatus.LOCKED) {
            Instant until = identity.getLockedUntil();
            if (until != null && until.isAfter(Instant.now())) {
                loginAttemptService.record(identity.getId(), identity.getEmail(), ctx.ip(),
                        ctx.userAgent(), LoginResult.LOCKED);
                throw new AccountLockedException("Account is locked until " + until);
            }
            // Lock window elapsed: auto-unlock.
            identity.setStatus(IdentityStatus.ACTIVE);
            identity.setFailedLoginCount(0);
            identity.setLockedUntil(null);
            identityRepository.save(identity);
        }
    }

    private void handleFailedPassword(Identity identity, RequestContext ctx) {
        int failures = identity.getFailedLoginCount() + 1;
        identity.setFailedLoginCount(failures);
        loginAttemptService.record(identity.getId(), identity.getEmail(), ctx.ip(),
                ctx.userAgent(), LoginResult.BAD_CREDENTIALS);

        if (failures >= lockout.getMaxFailedAttempts()) {
            Instant until = Instant.now().plus(lockout.getLockDuration());
            identity.setStatus(IdentityStatus.LOCKED);
            identity.setLockedUntil(until);
            identityRepository.save(identity);
            events.publish(AuthEvents.AccountLocked.of(identity.getId(), "too_many_failed_logins", until));
            throw new AccountLockedException("Account locked due to repeated failed logins");
        }
        identityRepository.save(identity);
    }

    private String touchDevice(Identity identity, String fingerprint, String label, RequestContext ctx) {
        DeviceService.DeviceResolution resolution =
                deviceService.registerOrTouch(identity.getId(), fingerprint, ctx.ip(), label);
        if (resolution.device() == null) {
            return null;
        }
        if (resolution.isNew()) {
            events.publish(AuthEvents.NewDeviceLogin.of(identity.getId(),
                    resolution.device().getId(), ctx.ip()));
        }
        return resolution.device().getId();
    }

    private TokenResponse issueSession(Identity identity, String deviceId, List<String> amr) {
        RefreshTokenService.Issued issued =
                refreshTokenService.issue(identity.getId(), null, deviceId);
        List<String> scopes = scopeResolver.defaultScopes(identity.getPrincipalType());
        JwtService.IssuedToken access = jwtService.issueAccessToken(
                identity.getId(), identity.getPrincipalType(), scopes, issued.familyId(), amr);
        return TokenResponse.bearer(access.value(), accessTtlSeconds, issued.rawToken(), issued.familyId());
    }

    private static String rateKey(String email, RequestContext ctx) {
        return (email == null ? "?" : email.toLowerCase()) + "|" + (ctx.ip() == null ? "?" : ctx.ip());
    }

    // Kept to signal intent; PrincipalType is embedded in tokens.
    @SuppressWarnings("unused")
    private PrincipalType typeOf(Identity identity) {
        return identity.getPrincipalType();
    }
}
