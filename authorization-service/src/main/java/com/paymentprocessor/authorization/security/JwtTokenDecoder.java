package com.paymentprocessor.authorization.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.authorization.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * Decodes and (optionally) verifies identity tokens issued by the Authentication Service.
 *
 * <p>Supports HMAC and RSA/EC signature verification. When signature verification is disabled
 * (local/dev only) the payload is decoded without validation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenDecoder {

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * @return the token claims, or {@code null} if the token is invalid.
     */
    public Map<String, Object> decode(String token) {
        try {
            if (!properties.isVerifySignature()) {
                return decodeWithoutVerification(token);
            }
            var parser = Jwts.parser();
            if (properties.getPublicKey() != null && !properties.getPublicKey().isBlank()) {
                parser.verifyWith(publicKey(properties.getPublicKey()));
            } else if (properties.getHmacSecret() != null && !properties.getHmacSecret().isBlank()) {
                parser.verifyWith(hmacKey(properties.getHmacSecret()));
            } else {
                throw new IllegalStateException(
                        "security.jwt.verify-signature is true but no key is configured");
            }
            if (properties.getIssuer() != null && !properties.getIssuer().isBlank()) {
                parser.requireIssuer(properties.getIssuer());
            }
            Claims claims = parser.build().parseSignedClaims(token).getPayload();
            return claims;
        } catch (Exception ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeWithoutVerification(String token) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
        return objectMapper.readValue(payload, Map.class);
    }

    private SecretKey hmacKey(String secret) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException notBase64) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private PublicKey publicKey(String pem) throws Exception {
        String normalized = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der));
    }
}
