package com.paymentprocessor.authenticationservice.service;

import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.domain.CredentialKind;
import com.paymentprocessor.authenticationservice.entity.Credential;
import com.paymentprocessor.authenticationservice.exception.BadRequestException;
import com.paymentprocessor.authenticationservice.repository.CredentialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns password credential storage: Argon2 hashing, rotation, history and
 * verification. Plaintext passwords never leave this class.
 */
@Service
public class CredentialService {

    private final CredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService policyService;
    private final AuthProperties.Password policy;

    public CredentialService(CredentialRepository repository,
                             PasswordEncoder passwordEncoder,
                             PasswordPolicyService policyService,
                             AuthProperties props) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.policyService = policyService;
        this.policy = props.getPassword();
    }

    @Transactional
    public void setPassword(String identityId, String rawPassword) {
        policyService.validate(rawPassword);
        enforceHistory(identityId, rawPassword);

        // Rotate the current active password, if any.
        repository.findByIdentityIdAndKindAndRotatedAtIsNull(identityId, CredentialKind.PASSWORD)
                .ifPresent(active -> {
                    active.setRotatedAt(Instant.now());
                    repository.save(active);
                });

        Credential credential = new Credential();
        credential.setId(UUID.randomUUID().toString());
        credential.setIdentityId(identityId);
        credential.setKind(CredentialKind.PASSWORD);
        credential.setSecretHash(passwordEncoder.encode(rawPassword));
        credential.setAlgoParams("argon2id");
        if (policy.getMaxAge() != null && !policy.getMaxAge().isZero()) {
            credential.setExpiresAt(Instant.now().plus(policy.getMaxAge()));
        }
        credential.setCreatedAt(Instant.now());
        repository.save(credential);
    }

    @Transactional(readOnly = true)
    public boolean verifyPassword(String identityId, String rawPassword) {
        Optional<Credential> active =
                repository.findByIdentityIdAndKindAndRotatedAtIsNull(identityId, CredentialKind.PASSWORD);
        return active.map(c -> passwordEncoder.matches(rawPassword, c.getSecretHash())).orElse(false);
    }

    private void enforceHistory(String identityId, String rawPassword) {
        if (policy.getHistoryDepth() <= 0) {
            return;
        }
        List<Credential> recent =
                repository.findByIdentityIdAndKindOrderByCreatedAtDesc(identityId, CredentialKind.PASSWORD);
        recent.stream()
                .limit(policy.getHistoryDepth())
                .filter(c -> passwordEncoder.matches(rawPassword, c.getSecretHash()))
                .findAny()
                .ifPresent(c -> {
                    throw new BadRequestException(
                            "Password must not match any of your last " + policy.getHistoryDepth() + " passwords");
                });
    }
}
