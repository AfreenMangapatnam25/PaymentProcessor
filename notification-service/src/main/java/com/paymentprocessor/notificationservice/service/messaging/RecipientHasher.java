package com.paymentprocessor.notificationservice.service.messaging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * messages.recipient_hash and suppressions.recipient_hash intentionally
 * never store raw PII (email address / phone number) -- only a SHA-256 hash
 * of the normalized recipient, scoped by channel so the same person's email
 * and SMS suppressions don't collide.
 *
 * Normalization: emails are lowercased/trimmed; phone numbers keep only
 * leading '+' and digits, which is enough to make "+1 (555) 123-4567" and
 * "+15551234567" hash identically without pulling in a full E.164 library.
 */
@Component
public class RecipientHasher {

    public byte[] hash(String channel, String rawRecipient) {
        String normalized = normalize(channel, rawRecipient);
        return sha256((channel + ":" + normalized).getBytes(StandardCharsets.UTF_8));
    }

    private String normalize(String channel, String rawRecipient) {
        if (rawRecipient == null) {
            throw new IllegalArgumentException("recipient must not be null");
        }
        if ("email".equalsIgnoreCase(channel)) {
            return rawRecipient.trim().toLowerCase(Locale.ROOT);
        }
        if ("sms".equalsIgnoreCase(channel)) {
            return rawRecipient.replaceAll("[^+0-9]", "");
        }
        return rawRecipient.trim();
    }

    private byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
