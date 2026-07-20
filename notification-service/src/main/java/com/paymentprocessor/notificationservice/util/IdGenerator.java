package com.paymentprocessor.notificationservice.util;

import java.security.SecureRandom;
import java.util.UUID;

/** Generates prefixed opaque ids in the style used throughout the schema (evt_..., msg_..., tmpl_..., ep_...). */
public final class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {
    }

    public static String generate(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    /** Shorter random suffix, useful where a UUID would be needlessly long (e.g. secrets). */
    public static String randomToken(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
