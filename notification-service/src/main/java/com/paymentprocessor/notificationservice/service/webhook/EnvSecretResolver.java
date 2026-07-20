package com.paymentprocessor.notificationservice.service.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Default, non-KMS SecretResolver used for local dev and tests
 * (notification.secrets.provider=env, the default).
 *
 * Resolution order for a given secret_ref:
 *  1. An explicit env var override: WEBHOOK_SECRET_<sanitized ref>.
 *  2. A deterministic secret derived from a local master key + the ref
 *     (SHA-256), so every endpoint still gets a distinct, stable signing
 *     secret without an operator having to provision one per endpoint.
 *
 * In production, set notification.secrets.provider to a real KMS-backed
 * implementation instead of relying on this class.
 */
@Component
public class EnvSecretResolver implements SecretResolver {

    private final String localMasterKey;

    public EnvSecretResolver(@Value("${notification.secrets.local-master-key:dev-only-insecure-master-key}") String localMasterKey) {
        this.localMasterKey = localMasterKey;
    }

    @Override
    public String resolve(String secretRef) {
        String envOverride = System.getenv(envVarName(secretRef));
        if (envOverride != null && !envOverride.isBlank()) {
            return envOverride;
        }
        return sha256Hex(localMasterKey + ":" + secretRef);
    }

    private static String envVarName(String secretRef) {
        return "WEBHOOK_SECRET_" + secretRef.replaceAll("[^A-Za-z0-9]", "_").toUpperCase();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
