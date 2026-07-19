package com.paymentprocessor.authenticationservice.service;

import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.domain.IdentityStatus;
import com.paymentprocessor.authenticationservice.entity.Identity;
import com.paymentprocessor.authenticationservice.entity.PasswordResetToken;
import com.paymentprocessor.authenticationservice.event.AuthEvents;
import com.paymentprocessor.authenticationservice.event.DomainEventPublisher;
import com.paymentprocessor.authenticationservice.exception.BadRequestException;
import com.paymentprocessor.authenticationservice.repository.IdentityRepository;
import com.paymentprocessor.authenticationservice.repository.PasswordResetTokenRepository;
import com.paymentprocessor.authenticationservice.security.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenRepository repository;
    private final IdentityRepository identityRepository;
    private final CredentialService credentialService;
    private final RefreshTokenService refreshTokenService;
    private final DomainEventPublisher events;
    private final TokenHasher hasher;
    private final AuthProperties.Reset config;

    public PasswordResetService(PasswordResetTokenRepository repository,
                                IdentityRepository identityRepository,
                                CredentialService credentialService,
                                RefreshTokenService refreshTokenService,
                                DomainEventPublisher events,
                                TokenHasher hasher,
                                AuthProperties props) {
        this.repository = repository;
        this.identityRepository = identityRepository;
        this.credentialService = credentialService;
        this.refreshTokenService = refreshTokenService;
        this.events = events;
        this.hasher = hasher;
        this.config = props.getReset();
    }

    /**
     * Creates a reset token if the email maps to an identity. Returns the raw
     * token when one was created (for Notification Service delivery). The caller
     * must always respond identically to avoid account enumeration.
     */
    @Transactional
    public Optional<String> requestReset(String email) {
        return identityRepository.findByEmailIgnoreCase(email).map(identity -> {
            String raw = hasher.randomToken(32);
            PasswordResetToken token = new PasswordResetToken();
            token.setId(UUID.randomUUID().toString());
            token.setIdentityId(identity.getId());
            token.setTokenHash(hasher.sha256Hex(raw));
            token.setExpiresAt(Instant.now().plus(config.getTokenTtl()));
            repository.save(token);
            log.debug("Issued password reset token for identity {}", identity.getId());
            return raw;
        });
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        PasswordResetToken token = repository.findByTokenHash(hasher.sha256Hex(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        credentialService.setPassword(token.getIdentityId(), newPassword);
        token.setUsedAt(Instant.now());
        repository.save(token);

        // Invalidate all sessions and clear any lockout.
        refreshTokenService.revokeAllForIdentity(token.getIdentityId());
        identityRepository.findById(token.getIdentityId()).ifPresent(this::clearLock);

        events.publish(AuthEvents.PasswordChanged.of(token.getIdentityId(), true));
    }

    private void clearLock(Identity identity) {
        identity.setFailedLoginCount(0);
        identity.setLockedUntil(null);
        if (identity.getStatus() == IdentityStatus.LOCKED) {
            identity.setStatus(IdentityStatus.ACTIVE);
        }
        identityRepository.save(identity);
    }
}
