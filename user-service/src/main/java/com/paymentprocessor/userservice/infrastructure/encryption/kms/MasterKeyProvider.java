package com.paymentprocessor.userservice.infrastructure.encryption.kms;

import javax.crypto.SecretKey;

/**
 * Wraps and unwraps per-subject DEKs with a master key (KEK) that never leaves
 * the KMS/HSM. The local implementation is for development only; production
 * binds an AWS KMS / GCP KMS / Vault implementation behind this same port.
 */
public interface MasterKeyProvider {

    /** Encrypt (wrap) a DEK for storage in {@code crypto_keys.wrapped_dek}. */
    byte[] wrap(SecretKey dek);

    /** Decrypt (unwrap) a stored DEK back into a usable key. */
    SecretKey unwrap(byte[] wrappedDek);

    /** Identifier of the active master key, recorded for rotation/audit. */
    String masterKeyId();
}
