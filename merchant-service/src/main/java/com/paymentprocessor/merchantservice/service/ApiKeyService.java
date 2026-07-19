package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.common.crypto.SecretHasher;
import com.paymentprocessor.merchantservice.common.enums.ApiKeyStatus;
import com.paymentprocessor.merchantservice.common.enums.ApiKeyType;
import com.paymentprocessor.merchantservice.common.error.BusinessRuleException;
import com.paymentprocessor.merchantservice.common.error.ResourceNotFoundException;
import com.paymentprocessor.merchantservice.dto.ApiKeyCreateRequest;
import com.paymentprocessor.merchantservice.dto.ApiKeyCreatedResponse;
import com.paymentprocessor.merchantservice.dto.ApiKeyResponse;
import com.paymentprocessor.merchantservice.entity.ApiKey;
import com.paymentprocessor.merchantservice.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Provisions and manages merchant API credentials. Only the public key id and a hash of the
 * secret are stored; the full secret ({@code <keyId>.<secret>}) is returned exactly once.
 */
@Service
public class ApiKeyService {

    private static final int SECRET_BYTES = 32;
    private static final String KEY_ID_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository repository;
    private final MerchantService merchantService;
    private final SecretHasher secretHasher;

    public ApiKeyService(ApiKeyRepository repository, MerchantService merchantService, SecretHasher secretHasher) {
        this.repository = repository;
        this.merchantService = merchantService;
        this.secretHasher = secretHasher;
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listActive(UUID merchantId) {
        merchantService.assertAccessible(merchantId);
        return repository.findByMerchantId(merchantId).stream()
                .filter(k -> k.getStatus() == ApiKeyStatus.ACTIVE)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ApiKeyCreatedResponse generate(UUID merchantId, ApiKeyCreateRequest req) {
        merchantService.assertAccessible(merchantId);
        String secret = secretHasher.generateSecret(SECRET_BYTES);
        ApiKey key = new ApiKey();
        key.setMerchantId(merchantId);
        key.setKeyId(generateKeyId(req.keyType()));
        key.setSecretHash(secretHasher.hash(secret));
        key.setKeyType(req.keyType());
        key.setStatus(ApiKeyStatus.ACTIVE);
        key.setLabel(req.label());
        key.setIpAllowlist(req.ipAllowlist());
        key.setExpiresAt(req.expiresAt());
        key = repository.save(key);
        return new ApiKeyCreatedResponse(toResponse(key), key.getKeyId() + "." + secret);
    }

    @Transactional
    public ApiKeyCreatedResponse rotate(UUID merchantId, UUID keyId) {
        merchantService.assertAccessible(merchantId);
        ApiKey existing = load(merchantId, keyId);
        if (existing.getStatus() != ApiKeyStatus.ACTIVE) {
            throw new BusinessRuleException("Only an active key can be rotated");
        }
        revokeInternal(existing);
        ApiKeyCreateRequest req = new ApiKeyCreateRequest(
                existing.getKeyType(), existing.getLabel(), existing.getIpAllowlist(), existing.getExpiresAt());
        return generate(merchantId, req);
    }

    @Transactional
    public void revoke(UUID merchantId, UUID keyId) {
        merchantService.assertAccessible(merchantId);
        revokeInternal(load(merchantId, keyId));
    }

    @Transactional
    public ApiKeyResponse configureIpAllowlist(UUID merchantId, UUID keyId, String ipAllowlist) {
        merchantService.assertAccessible(merchantId);
        ApiKey key = load(merchantId, keyId);
        key.setIpAllowlist(ipAllowlist);
        return toResponse(key);
    }

    private void revokeInternal(ApiKey key) {
        key.setStatus(ApiKeyStatus.REVOKED);
        key.setRevokedAt(Instant.now());
    }

    private ApiKey load(UUID merchantId, UUID keyId) {
        return repository.findByIdAndMerchantId(keyId, merchantId)
                .orElseThrow(() -> ResourceNotFoundException.of("ApiKey", keyId));
    }

    private String generateKeyId(ApiKeyType type) {
        String prefix = switch (type) {
            case PRODUCTION -> "pk_live_";
            case SANDBOX -> "pk_test_";
            case READ_ONLY -> "pk_ro_";
        };
        String keyId;
        do {
            StringBuilder sb = new StringBuilder(prefix);
            for (int i = 0; i < 24; i++) {
                sb.append(KEY_ID_ALPHABET.charAt(RANDOM.nextInt(KEY_ID_ALPHABET.length())));
            }
            keyId = sb.toString();
        } while (repository.findByKeyId(keyId).isPresent());
        return keyId;
    }

    private ApiKeyResponse toResponse(ApiKey k) {
        return new ApiKeyResponse(k.getId(), k.getMerchantId(), k.getKeyId(), k.getKeyType(), k.getStatus(),
                k.getLabel(), k.getIpAllowlist(), k.getLastUsedAt(), k.getExpiresAt(), k.getCreatedAt());
    }
}
