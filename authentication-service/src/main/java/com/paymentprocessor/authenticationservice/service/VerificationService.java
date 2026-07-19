package com.paymentprocessor.authenticationservice.service;

import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.domain.VerificationChannel;
import com.paymentprocessor.authenticationservice.entity.Identity;
import com.paymentprocessor.authenticationservice.entity.VerificationToken;
import com.paymentprocessor.authenticationservice.exception.BadRequestException;
import com.paymentprocessor.authenticationservice.repository.IdentityRepository;
import com.paymentprocessor.authenticationservice.repository.VerificationTokenRepository;
import com.paymentprocessor.authenticationservice.security.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Email/phone ownership verification. The raw token/code is returned to the
 * caller for delivery via the Notification Service; it is never persisted in the
 * clear (only its hash is stored).
 */
@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VerificationTokenRepository repository;
    private final IdentityRepository identityRepository;
    private final TokenHasher hasher;
    private final AuthProperties.Verification config;

    public VerificationService(VerificationTokenRepository repository,
                               IdentityRepository identityRepository,
                               TokenHasher hasher,
                               AuthProperties props) {
        this.repository = repository;
        this.identityRepository = identityRepository;
        this.hasher = hasher;
        this.config = props.getVerification();
    }

    public record StartResult(String token, String code) {}

    @Transactional
    public StartResult start(String identityId, VerificationChannel channel) {
        Identity identity = identityRepository.findById(identityId)
                .orElseThrow(() -> new BadRequestException("Unknown identity"));
        String destination = channel == VerificationChannel.EMAIL
                ? identity.getEmail() : identity.getPhoneE164();
        if (destination == null || destination.isBlank()) {
            throw new BadRequestException("No " + channel.name().toLowerCase() + " on file to verify");
        }

        String rawToken = hasher.randomToken(24);
        String code = channel == VerificationChannel.PHONE ? sixDigits() : null;

        VerificationToken token = new VerificationToken();
        token.setId(UUID.randomUUID().toString());
        token.setIdentityId(identityId);
        token.setChannel(channel);
        token.setDestination(destination);
        token.setTokenHash(hasher.sha256Hex(rawToken));
        token.setCode(code);
        token.setExpiresAt(Instant.now().plus(config.getTokenTtl()));
        repository.save(token);

        log.debug("Issued {} verification token for identity {}", channel, identityId);
        return new StartResult(rawToken, code);
    }

    @Transactional
    public void confirm(String rawToken, String code) {
        VerificationToken token = repository.findByTokenHash(hasher.sha256Hex(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));
        if (token.getConsumedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired verification token");
        }
        if (token.getChannel() == VerificationChannel.PHONE
                && (code == null || !code.equals(token.getCode()))) {
            throw new BadRequestException("Invalid verification code");
        }

        token.setConsumedAt(Instant.now());
        repository.save(token);

        Identity identity = identityRepository.findById(token.getIdentityId())
                .orElseThrow(() -> new BadRequestException("Unknown identity"));
        if (token.getChannel() == VerificationChannel.EMAIL) {
            identity.setEmailVerifiedAt(Instant.now());
        } else {
            identity.setPhoneVerifiedAt(Instant.now());
        }
        identityRepository.save(identity);
    }

    private static String sixDigits() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
