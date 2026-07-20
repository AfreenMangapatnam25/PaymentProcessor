package com.paymentprocessor.userservice.application.port.out;

/**
 * Outbound port (owned by the application layer) for GDPR crypto-shredding.
 * Implemented by infrastructure. Keeps application services free of any
 * encryption/KMS detail -- they just ask for a subject to be shredded.
 */
public interface CryptoShredderPort {

    /**
     * Destroy the encryption key for a subject, rendering its PII ciphertext
     * permanently unrecoverable. Idempotent.
     *
     * @return true if a key was destroyed, false if none was active
     */
    boolean shred(String subjectType, String subjectId);
}
