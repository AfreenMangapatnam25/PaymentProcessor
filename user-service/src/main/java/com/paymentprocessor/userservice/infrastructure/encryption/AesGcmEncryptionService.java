package com.paymentprocessor.userservice.infrastructure.encryption;

import com.paymentprocessor.userservice.common.constants.ErrorCode;
import com.paymentprocessor.userservice.common.exception.InfrastructureException;
import com.paymentprocessor.userservice.infrastructure.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES-GCM authenticated field encryption. Layout of the returned blob:
 * {@code [12-byte nonce][ciphertext][16-byte GCM tag]}. A fresh random nonce is
 * generated per encryption (never reused for a given key), and GCM's tag gives
 * tamper detection. Transformation and tag length come from configuration.
 */
@Service
public class AesGcmEncryptionService implements EncryptionService {

    private static final int NONCE_LENGTH_BYTES = 12;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String transformation;
    private final int tagLengthBits;

    public AesGcmEncryptionService(AppProperties properties) {
        AppProperties.Crypto.DataKey dataKey = properties.crypto().dataKey();
        this.transformation = dataKey.cipherTransformation();
        this.tagLengthBits = dataKey.gcmTagLengthBits();
    }

    @Override
    public byte[] encrypt(byte[] plaintext, SecretKey dek) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.ENCRYPT_MODE, dek, new GCMParameterSpec(tagLengthBits, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] out = new byte[nonce.length + ciphertext.length];
            System.arraycopy(nonce, 0, out, 0, nonce.length);
            System.arraycopy(ciphertext, 0, out, nonce.length, ciphertext.length);
            return out;
        } catch (Exception e) {
            // Never include the plaintext in the error (rule 13).
            throw InfrastructureException.encryptionFailed("field-encrypt", e);
        }
    }

    @Override
    public byte[] decrypt(byte[] blob, SecretKey dek) {
        if (blob == null) {
            return null;
        }
        if (blob.length <= NONCE_LENGTH_BYTES) {
            throw new InfrastructureException(ErrorCode.ENCRYPTION_ERROR, "Ciphertext is too short to contain a nonce");
        }
        try {
            byte[] nonce = Arrays.copyOfRange(blob, 0, NONCE_LENGTH_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(blob, NONCE_LENGTH_BYTES, blob.length);

            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.DECRYPT_MODE, dek, new GCMParameterSpec(tagLengthBits, nonce));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw InfrastructureException.encryptionFailed("field-decrypt", e);
        }
    }
}
