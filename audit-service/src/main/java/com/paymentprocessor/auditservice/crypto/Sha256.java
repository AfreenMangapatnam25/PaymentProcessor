package com.paymentprocessor.auditservice.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** SHA-256 helpers producing the {@code sha256:<hex>} form used throughout the chain. */
public final class Sha256 {

    public static final String PREFIX = "sha256:";

    private Sha256() {
    }

    public static byte[] digest(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed present on every JVM.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static String hashPrefixed(String input) {
        return PREFIX + toHex(digest(input.getBytes(StandardCharsets.UTF_8)));
    }

    public static String hashPrefixed(byte[] input) {
        return PREFIX + toHex(digest(input));
    }

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
