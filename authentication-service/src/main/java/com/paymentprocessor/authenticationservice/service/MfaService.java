package com.paymentprocessor.authenticationservice.service;

import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.domain.MfaKind;
import com.paymentprocessor.authenticationservice.domain.MfaStatus;
import com.paymentprocessor.authenticationservice.dto.MfaEnrollResponse;
import com.paymentprocessor.authenticationservice.entity.MfaFactor;
import com.paymentprocessor.authenticationservice.entity.MfaRecoveryCode;
import com.paymentprocessor.authenticationservice.event.AuthEvents;
import com.paymentprocessor.authenticationservice.event.DomainEventPublisher;
import com.paymentprocessor.authenticationservice.exception.BadRequestException;
import com.paymentprocessor.authenticationservice.exception.NotFoundException;
import com.paymentprocessor.authenticationservice.mfa.TotpService;
import com.paymentprocessor.authenticationservice.repository.MfaFactorRepository;
import com.paymentprocessor.authenticationservice.repository.MfaRecoveryCodeRepository;
import com.paymentprocessor.authenticationservice.security.TokenHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MfaService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final MfaFactorRepository factorRepository;
    private final MfaRecoveryCodeRepository recoveryRepository;
    private final TotpService totpService;
    private final TokenHasher hasher;
    private final DomainEventPublisher events;
    private final int recoveryCodeCount;

    public MfaService(MfaFactorRepository factorRepository,
                      MfaRecoveryCodeRepository recoveryRepository,
                      TotpService totpService,
                      TokenHasher hasher,
                      DomainEventPublisher events,
                      AuthProperties props) {
        this.factorRepository = factorRepository;
        this.recoveryRepository = recoveryRepository;
        this.totpService = totpService;
        this.hasher = hasher;
        this.events = events;
        this.recoveryCodeCount = props.getMfa().getRecoveryCodeCount();
    }

    @Transactional
    public MfaEnrollResponse enrollTotp(String identityId, String accountName, String label) {
        String secret = totpService.generateSecret();
        MfaFactor factor = new MfaFactor();
        factor.setId(UUID.randomUUID().toString());
        factor.setIdentityId(identityId);
        factor.setKind(MfaKind.TOTP);
        factor.setSecretRef(secret);
        factor.setLabel(label);
        factor.setStatus(MfaStatus.PENDING);
        factorRepository.save(factor);
        String uri = totpService.provisioningUri(secret, accountName != null ? accountName : identityId);
        return new MfaEnrollResponse(factor.getId(), secret, uri);
    }

    @Transactional
    public void verifyEnrollment(String identityId, String factorId, String code) {
        MfaFactor factor = loadOwned(identityId, factorId);
        if (factor.getStatus() == MfaStatus.ACTIVE) {
            return;
        }
        if (!totpService.verify(factor.getSecretRef(), code)) {
            throw new BadRequestException("Invalid verification code");
        }
        factor.setStatus(MfaStatus.ACTIVE);
        factor.setVerifiedAt(Instant.now());
        factorRepository.save(factor);
        events.publish(AuthEvents.MfaEnabled.of(identityId, factor.getKind().name()));
    }

    @Transactional
    public void disable(String identityId, String factorId) {
        MfaFactor factor = loadOwned(identityId, factorId);
        factor.setStatus(MfaStatus.DISABLED);
        factorRepository.save(factor);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveMfa(String identityId) {
        return factorRepository.existsByIdentityIdAndStatus(identityId, MfaStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<String> activeMethods(String identityId) {
        return factorRepository.findByIdentityIdAndStatus(identityId, MfaStatus.ACTIVE).stream()
                .map(f -> f.getKind().name()).distinct().toList();
    }

    /** Verifies a login-time challenge against any active TOTP factor, then recovery codes. */
    @Transactional
    public boolean verifyChallenge(String identityId, String code) {
        boolean totpOk = factorRepository.findByIdentityIdAndStatus(identityId, MfaStatus.ACTIVE).stream()
                .filter(f -> f.getKind() == MfaKind.TOTP)
                .anyMatch(f -> totpService.verify(f.getSecretRef(), code));
        if (totpOk) {
            return true;
        }
        return consumeRecoveryCode(identityId, code);
    }

    @Transactional
    public List<String> regenerateRecoveryCodes(String identityId) {
        recoveryRepository.deleteByIdentityId(identityId);
        List<String> plain = new ArrayList<>();
        for (int i = 0; i < recoveryCodeCount; i++) {
            String code = randomRecoveryCode();
            plain.add(code);
            MfaRecoveryCode entity = new MfaRecoveryCode();
            entity.setId(UUID.randomUUID().toString());
            entity.setIdentityId(identityId);
            entity.setCodeHash(hasher.sha256Hex(normalize(code)));
            recoveryRepository.save(entity);
        }
        return plain;
    }

    private boolean consumeRecoveryCode(String identityId, String code) {
        if (code == null) {
            return false;
        }
        return recoveryRepository
                .findByIdentityIdAndCodeHashAndUsedAtIsNull(identityId, hasher.sha256Hex(normalize(code)))
                .map(rc -> {
                    rc.setUsedAt(Instant.now());
                    recoveryRepository.save(rc);
                    return true;
                }).orElse(false);
    }

    private MfaFactor loadOwned(String identityId, String factorId) {
        MfaFactor factor = factorRepository.findById(factorId)
                .orElseThrow(() -> new NotFoundException("MFA factor not found"));
        if (!factor.getIdentityId().equals(identityId)) {
            throw new NotFoundException("MFA factor not found");
        }
        return factor;
    }

    private static String randomRecoveryCode() {
        StringBuilder sb = new StringBuilder(11);
        for (int i = 0; i < 10; i++) {
            if (i == 5) sb.append('-');
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String normalize(String code) {
        return code.replace("-", "").trim().toUpperCase(Locale.ROOT);
    }
}
