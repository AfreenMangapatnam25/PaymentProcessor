package com.paymentprocessor.userservice.infrastructure.encryption;

import com.paymentprocessor.userservice.common.exception.InfrastructureException;
import com.paymentprocessor.userservice.infrastructure.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Produces deterministic blind indexes for encrypted columns. Because AES-GCM
 * ciphertext is non-deterministic and unsearchable, we store HMAC-SHA256(value)
 * alongside it: equal plaintexts yield equal indexes (enabling uniqueness and
 * equality lookup) while the index is irreversible without the HMAC key.
 *
 * <p>Trade-off (accepted): a deterministic index leaks equality between rows.
 * We mitigate by normalizing before hashing and keeping the key in the KMS/secret
 * store. This is the standard searchable-encryption compromise.
 */
@Service
public class BlindIndexService {

    private final String hmacAlgorithm;
    private final SecretKeySpec indexKey;

    public BlindIndexService(AppProperties properties) {
        AppProperties.Crypto.BlindIndex cfg = properties.crypto().blindIndex();
        this.hmacAlgorithm = cfg.hmacAlgorithm();
        this.indexKey = deriveKey(cfg.keyId(), this.hmacAlgorithm);
    }

    /**
     * Compute the blind index of an already-normalized value (e.g. Email/Phone
     * value objects normalize on construction). Returns null for null input.
     */
    public String index(String normalizedValue) {
        if (normalizedValue == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(hmacAlgorithm);
            mac.init(indexKey);
            byte[] digest = mac.doFinal(normalizedValue.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw InfrastructureException.encryptionFailed("blind-index", e);
        }
    }

    private static SecretKeySpec deriveKey(String seed, String algorithm) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(seed.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, algorithm);
        } catch (Exception e) {
            throw InfrastructureException.encryptionFailed("blind-index-key-derive", e);
        }
    }
}
