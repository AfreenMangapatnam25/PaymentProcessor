package com.paymentprocessor.authenticationservice.service;

import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import com.paymentprocessor.authenticationservice.dto.ApiKeyCreateRequest;
import com.paymentprocessor.authenticationservice.dto.ApiKeyCreatedResponse;
import com.paymentprocessor.authenticationservice.dto.ApiKeyResponse;
import com.paymentprocessor.authenticationservice.dto.ApiKeyVerifyResponse;
import com.paymentprocessor.authenticationservice.entity.ApiKey;
import com.paymentprocessor.authenticationservice.exception.NotFoundException;
import com.paymentprocessor.authenticationservice.repository.ApiKeyRepository;
import com.paymentprocessor.authenticationservice.security.TokenHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service-to-service API keys. The full key is {@code <prefix>.<secret>}; only
 * the SHA-256 of the secret is stored. The plaintext is shown exactly once.
 */
@Service
public class ApiKeyService {

    private final ApiKeyRepository repository;
    private final TokenHasher hasher;

    public ApiKeyService(ApiKeyRepository repository, TokenHasher hasher) {
        this.repository = repository;
        this.hasher = hasher;
    }

    @Transactional
    public ApiKeyCreatedResponse create(ApiKeyCreateRequest req) {
        String prefix = "pk_" + sanitize(req.environment()) + "_" + hasher.randomToken(6).substring(0, 8);
        String secret = hasher.randomToken(24);

        ApiKey key = new ApiKey();
        key.setId(UUID.randomUUID().toString());
        key.setOwnerType(req.ownerType());
        key.setOwnerId(req.ownerId());
        key.setPrefix(prefix);
        key.setSecretHash(hasher.sha256Hex(secret));
        key.setScopes(req.scopes() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(req.scopes()));
        key.setEnvironment(req.environment());
        key.setExpiresAt(req.expiresAt());
        repository.save(key);

        return new ApiKeyCreatedResponse(ApiKeyResponse.from(key), prefix + "." + secret);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> list(PrincipalType ownerType, String ownerId) {
        return repository.findByOwnerTypeAndOwnerId(ownerType, ownerId).stream()
                .map(ApiKeyResponse::from).toList();
    }

    @Transactional
    public void revoke(String id) {
        ApiKey key = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("API key not found"));
        if (key.getRevokedAt() == null) {
            key.setRevokedAt(Instant.now());
            repository.save(key);
        }
    }

    @Transactional
    public ApiKeyVerifyResponse verify(String fullKey) {
        int dot = fullKey == null ? -1 : fullKey.lastIndexOf('.');
        if (dot < 1) {
            return ApiKeyVerifyResponse.invalid();
        }
        String prefix = fullKey.substring(0, dot);
        String secret = fullKey.substring(dot + 1);

        return repository.findByPrefix(prefix)
                .filter(k -> k.isActive(Instant.now()))
                .filter(k -> hasher.matches(secret, k.getSecretHash()))
                .map(k -> {
                    k.setLastUsedAt(Instant.now());
                    repository.save(k);
                    return new ApiKeyVerifyResponse(true, k.getId(), k.getOwnerType(),
                            k.getOwnerId(), Set.copyOf(k.getScopes()), k.getEnvironment());
                })
                .orElse(ApiKeyVerifyResponse.invalid());
    }

    private static String sanitize(String env) {
        return env == null ? "env" : env.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }
}
