package com.paymentprocessor.auditservice.common;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Minimal ULID generator (Crockford base32, 48-bit timestamp + 80-bit randomness).
 * ULIDs are lexicographically sortable by creation time, which is why audit ids look
 * like {@code aud_01J...} and sort naturally in the collection.
 */
public final class Ulid {

    private static final char[] ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private Ulid() {
    }

    /** Generates a 26-character ULID string. */
    public static String generate() {
        return generate(Instant.now());
    }

    public static String generate(Instant time) {
        long timestamp = time.toEpochMilli();
        byte[] randomness = new byte[10];
        RANDOM.nextBytes(randomness);

        char[] out = new char[26];

        // 48-bit timestamp -> first 10 chars.
        long ts = timestamp;
        for (int i = 9; i >= 0; i--) {
            out[i] = ENCODING[(int) (ts & 0x1F)];
            ts >>>= 5;
        }

        // 80-bit randomness -> last 16 chars, encoded 5 bits at a time.
        int bitBuffer = 0;
        int bitsLeft = 0;
        int outIndex = 10;
        for (int i = 0; i < randomness.length && outIndex < 26; i++) {
            bitBuffer = (bitBuffer << 8) | (randomness[i] & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5 && outIndex < 26) {
                bitsLeft -= 5;
                out[outIndex++] = ENCODING[(bitBuffer >>> bitsLeft) & 0x1F];
            }
        }
        while (outIndex < 26) {
            out[outIndex++] = ENCODING[(bitBuffer << (5 - bitsLeft)) & 0x1F];
            bitsLeft = 0;
        }
        return new String(out);
    }
}
