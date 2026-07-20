package com.paymentprocessor.authorization.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.authorization.domain.payment.IdempotencyKey;
import com.paymentprocessor.authorization.exception.DuplicateRequestException;
import com.paymentprocessor.authorization.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent idempotency for authorization creation. The first request under a key records a hash
 * of its payload and the resulting resource id; replays return the stored resource, and a reused
 * key with a different payload is rejected with 409.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    /** SHA-256 hash of the canonical JSON form of a request payload. */
    public String hash(Object payload) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash idempotency payload", e);
        }
    }

    /**
     * @return the resource id previously created under {@code key} when the payload matches;
     *         empty when the key is unseen.
     * @throws DuplicateRequestException when the key was used with a different payload.
     */
    @Transactional(readOnly = true)
    public Optional<UUID> findExisting(String key, String requestHash) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return repository.findById(key).map(existing -> {
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new DuplicateRequestException(
                        "Idempotency-Key '" + key + "' was already used with a different request payload");
            }
            return existing.getResourceId();
        });
    }

    /** Persist the mapping from idempotency key to created resource. */
    @Transactional
    public void record(String key, String requestHash, String resourceType, UUID resourceId) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            repository.save(IdempotencyKey.builder()
                    .key(key)
                    .requestHash(requestHash)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .expiresAt(Instant.now().plus(TTL))
                    .build());
        } catch (DataIntegrityViolationException raced) {
            log.debug("Idempotency key {} already recorded concurrently", key);
        }
    }

    @Transactional
    public int purgeExpired() {
        return repository.deleteExpired(Instant.now());
    }

    @SuppressWarnings("unused")
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
