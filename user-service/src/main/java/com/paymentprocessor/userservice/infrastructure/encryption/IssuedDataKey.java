package com.paymentprocessor.userservice.infrastructure.encryption;

import javax.crypto.SecretKey;

/**
 * The result of issuing a new per-subject DEK: the persisted key's id (to store
 * as {@code crypto_key_id} on the owning row) plus the plaintext key for
 * immediate in-memory encryption. The plaintext {@code dek} is never persisted.
 */
public record IssuedDataKey(String cryptoKeyId, SecretKey dek) {
}
