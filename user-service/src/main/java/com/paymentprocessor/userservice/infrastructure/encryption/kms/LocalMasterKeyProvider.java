package com.paymentprocessor.userservice.infrastructure.encryption.kms;

import com.paymentprocessor.userservice.common.exception.InfrastructureException;
import com.paymentprocessor.userservice.infrastructure.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * DEVELOPMENT-ONLY master key provider. Derives a deterministic AES-256 KEK
 * from the configured master-key-id via SHA-256 and wraps DEKs with AES-GCM.
 * Active only when {@code app.crypto.kms.provider=local} (the default). In
 * dev/prod a real KMS-backed provider replaces this bean; nothing else changes.
 */
@Component
@ConditionalOnProperty(name = "app.crypto.kms.provider", havingValue = "local", matchIfMissing = true)
public class LocalMasterKeyProvider implements MasterKeyProvider {

    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey masterKey;
    private final String masterKeyId;

    public LocalMasterKeyProvider(AppProperties properties) {
        this.masterKeyId = properties.crypto().kms().masterKeyId();
        this.masterKey = deriveMasterKey(this.masterKeyId);
    }

    @Override
    public byte[] wrap(SecretKey dek) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] wrapped = cipher.doFinal(dek.getEncoded());

            byte[] out = new byte[nonce.length + wrapped.length];
            System.arraycopy(nonce, 0, out, 0, nonce.length);
            System.arraycopy(wrapped, 0, out, nonce.length, wrapped.length);
            return out;
        } catch (Exception e) {
            throw InfrastructureException.encryptionFailed("dek-wrap", e);
        }
    }

    @Override
    public SecretKey unwrap(byte[] wrappedDek) {
        try {
            byte[] nonce = Arrays.copyOfRange(wrappedDek, 0, NONCE_LENGTH_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(wrappedDek, NONCE_LENGTH_BYTES, wrappedDek.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] raw = cipher.doFinal(ciphertext);
            return new SecretKeySpec(raw, "AES");
        } catch (Exception e) {
            throw InfrastructureException.encryptionFailed("dek-unwrap", e);
        }
    }

    @Override
    public String masterKeyId() {
        return masterKeyId;
    }

    private static SecretKey deriveMasterKey(String seed) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(seed.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES"); // 32 bytes -> AES-256
        } catch (Exception e) {
            throw InfrastructureException.encryptionFailed("master-key-derive", e);
        }
    }
}
