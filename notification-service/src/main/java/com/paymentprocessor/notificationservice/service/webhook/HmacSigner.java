package com.paymentprocessor.notificationservice.service.webhook;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Signs outbound webhook payloads the way Stripe/GitHub-style webhooks do:
 * HMAC-SHA256 over "{timestamp}.{body}", so the receiver can bind the
 * signature to both the exact payload and a timestamp (defends against
 * replay when combined with a freshness check on the receiving end).
 *
 * Header shape: "t={epochSeconds},v1={hex hmac}"
 */
@Component
public class HmacSigner {

    private static final String ALGORITHM = "HmacSHA256";

    public String sign(String secret, String body, Instant timestamp) {
        long ts = timestamp.getEpochSecond();
        String signedPayload = ts + "." + body;
        String hex = hmacHex(secret, signedPayload);
        return "t=" + ts + ",v1=" + hex;
    }

    public boolean verify(String secret, String body, String header) {
        Long timestamp = null;
        String v1 = null;
        for (String part : header.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            if ("t".equals(kv[0])) timestamp = Long.parseLong(kv[1]);
            if ("v1".equals(kv[0])) v1 = kv[1];
        }
        if (timestamp == null || v1 == null) {
            return false;
        }
        String expected = hmacHex(secret, timestamp + "." + body);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                v1.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacHex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("failed to compute HMAC signature", e);
        }
    }
}
