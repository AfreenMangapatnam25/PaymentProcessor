package com.paymentprocessor.userservice.infrastructure.encryption;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Symmetric field-level encryption. Callers pass the plaintext and the
 * per-subject Data Encryption Key (DEK); the service returns self-describing
 * ciphertext (nonce prepended). Decrypting with a DEK that has been destroyed
 * is impossible -- that is precisely the GDPR crypto-shred guarantee.
 */
public interface EncryptionService {

    byte[] encrypt(byte[] plaintext, SecretKey dek);

    byte[] decrypt(byte[] ciphertext, SecretKey dek);

    default byte[] encryptString(String plaintext, SecretKey dek) {
        return plaintext == null ? null : encrypt(plaintext.getBytes(StandardCharsets.UTF_8), dek);
    }

    default String decryptToString(byte[] ciphertext, SecretKey dek) {
        return ciphertext == null ? null : new String(decrypt(ciphertext, dek), StandardCharsets.UTF_8);
    }
}
