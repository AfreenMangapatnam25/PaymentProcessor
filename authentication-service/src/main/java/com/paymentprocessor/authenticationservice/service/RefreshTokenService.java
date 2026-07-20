package com.paymentprocessor.authenticationservice.service;

import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.entity.RefreshToken;
import com.paymentprocessor.authenticationservice.exception.UnauthorizedException;
import com.paymentprocessor.authenticationservice.repository.RefreshTokenRepository;
import com.paymentprocessor.authenticationservice.security.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Opaque refresh tokens with rotation-on-use and automatic reuse detection.
 * Presenting an already-revoked token revokes the entire token family, defeating
 * stolen-token replay.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final TokenHasher hasher;
    private final Duration ttl;

    public RefreshTokenService(RefreshTokenRepository repository, TokenHasher hasher, AuthProperties props) {
        this.repository = repository;
        this.hasher = hasher;
        this.ttl = props.getRefresh().getTtl();
    }

    public record Issued(String rawToken, String familyId, Instant expiresAt) {}

    public record Rotation(String rawToken, String identityId, String familyId,
                           String deviceId, Instant expiresAt) {}

    @Transactional
    public Issued issue(String identityId, String familyId, String deviceId) {
        String raw = hasher.randomToken(TOKEN_BYTES);
        Instant now = Instant.now();
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID().toString());
        token.setIdentityId(identityId);
        token.setFamilyId(familyId != null ? familyId : UUID.randomUUID().toString());
        token.setTokenHash(hasher.sha256Hex(raw));
        token.setDeviceId(deviceId);
        token.setIssuedAt(now);
        token.setExpiresAt(now.plus(ttl));
        repository.save(token);
        return new Issued(raw, token.getFamilyId(), token.getExpiresAt());
    }

    @Transactional
    public Rotation rotate(String rawToken) {
        String hash = hasher.sha256Hex(rawToken);
        RefreshToken current = repository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        Instant now = Instant.now();
        if (current.getRevokedAt() != null) {
            // Reuse of a revoked token -> assume compromise, kill the family.
            repository.revokeFamily(current.getFamilyId(), now);
            log.warn("Refresh token reuse detected for family {}; family revoked", current.getFamilyId());
            throw new UnauthorizedException("Refresh token reuse detected");
        }
        if (current.getExpiresAt().isBefore(now)) {
            throw new UnauthorizedException("Refresh token expired");
        }

        Issued next = issue(current.getIdentityId(), current.getFamilyId(), current.getDeviceId());
        current.setRevokedAt(now);
        current.setReplacedBy(next.rawToken() == null ? null : hasher.sha256Hex(next.rawToken()));
        repository.save(current);

        return new Rotation(next.rawToken(), current.getIdentityId(), current.getFamilyId(),
                current.getDeviceId(), next.expiresAt());
    }

    public record LogoutInfo(String identityId, String familyId) {}

    @Transactional
    public Optional<LogoutInfo> revoke(String rawToken) {
        String hash = hasher.sha256Hex(rawToken);
        Optional<RefreshToken> found = repository.findByTokenHash(hash);
        found.ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                repository.save(token);
            }
        });
        return found.map(t -> new LogoutInfo(t.getIdentityId(), t.getFamilyId()));
    }

    @Transactional
    public int revokeAllForIdentity(String identityId) {
        return repository.revokeAllForIdentity(identityId, Instant.now());
    }
}
