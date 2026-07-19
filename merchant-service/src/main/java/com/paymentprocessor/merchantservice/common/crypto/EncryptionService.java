package com.paymentprocessor.merchantservice.common.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM authenticated encryption for sensitive fields at rest (e.g. bank account numbers).
 * Ciphertext is stored as Base64(iv || cipherText||tag). The key is a Base64-encoded 256-bit key
 * supplied per-environment via configuration; it must never be committed for production use.
 */
@Service
public class EncryptionService {

    private static final int IV_LENGTH = 12;       // 96-bit nonce recommended for GCM
    private static final int TAG_LENGTH_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] keyBytes;

    public EncryptionService(@Value("${merchant-service.security.encryption-key}") String base64Key) {
        this.keyBytes = Base64.getDecoder().decode(base64Key);
    }

    @PostConstruct
    void validateKey() {
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "merchant-service.security.encryption-key must decode to 32 bytes (256-bit AES key)");
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] out = ByteBuffer.allocate(iv.length + cipherText.length).put(iv).put(cipherText).array();
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            ByteBuffer buffer = ByteBuffer.wrap(all);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}
