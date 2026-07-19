package com.paymentprocessor.merchantservice.common.crypto;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hashes high-entropy secrets (API key secrets, webhook signing secrets) with SHA-256 for
 * O(1) verification. Secrets are randomly generated with sufficient entropy, so a per-secret
 * salt is unnecessary; comparison is constant-time to avoid timing side channels.
 */
@Component
public class SecretHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Generates a URL-safe random secret with the given number of random bytes. */
    public String generateSecret(int numBytes) {
        byte[] bytes = new byte[numBytes];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Hashing failed", e);
        }
    }

    public boolean matches(String secret, String expectedHash) {
        if (secret == null || expectedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(secret).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
