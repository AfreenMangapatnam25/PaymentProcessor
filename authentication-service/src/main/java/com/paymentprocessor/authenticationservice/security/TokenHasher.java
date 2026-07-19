package com.paymentprocessor.authenticationservice.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generates high-entropy opaque tokens (refresh tokens, reset/verification
 * tokens, API-key secrets) and hashes them with SHA-256 for at-rest storage.
 * Raw secrets are never persisted — only their hashes.
 */
@Component
public class TokenHasher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL = Base64.getUrlEncoder().withoutPadding();

    /** Returns a URL-safe random token with the requested number of entropy bytes. */
    public String randomToken(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return URL.encodeToString(buf);
    }

    /** SHA-256 hex digest, used as the stored lookup key for opaque tokens. */
    public String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Constant-time comparison of two hex hashes. */
    public boolean matches(String rawToken, String storedHash) {
        String candidate = sha256Hex(rawToken);
        return MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}
