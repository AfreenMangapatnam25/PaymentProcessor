package com.paymentprocessor.authenticationservice.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.paymentprocessor.authenticationservice.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * Provides the RSA signing key used for RS256 access tokens and exposes the
 * corresponding public JWK set consumed by the gateway-service.
 *
 * <p>Keys are loaded from PEM configuration ({@code JWT_PRIVATE_KEY} /
 * {@code JWT_PUBLIC_KEY}). If both are blank an ephemeral 2048-bit keypair is
 * generated at startup — acceptable for local dev only, never production.</p>
 */
@Component
public class RsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);

    private final RSAKey rsaKey;
    private final JWKSet publicJwkSet;

    public RsaKeyProvider(AuthProperties props) {
        AuthProperties.Jwt cfg = props.getJwt();
        RSAPublicKey publicKey;
        RSAPrivateKey privateKey;

        if (hasText(cfg.getPrivateKeyPem()) && hasText(cfg.getPublicKeyPem())) {
            publicKey = parsePublicKey(cfg.getPublicKeyPem());
            privateKey = parsePrivateKey(cfg.getPrivateKeyPem());
            log.info("Loaded RSA signing key from configured PEM material.");
        } else {
            KeyPair kp = generateKeyPair();
            publicKey = (RSAPublicKey) kp.getPublic();
            privateKey = (RSAPrivateKey) kp.getPrivate();
            log.warn("No RSA key configured (JWT_PRIVATE_KEY/JWT_PUBLIC_KEY). "
                    + "Generated an EPHEMERAL keypair — tokens will not survive a restart. "
                    + "Configure persistent keys before production.");
        }

        try {
            RSAKey.Builder builder = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256);
            RSAKey partial = builder.build();
            String kid = hasText(cfg.getKeyId())
                    ? cfg.getKeyId()
                    : partial.computeThumbprint().toString();
            this.rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyID(kid)
                    .build();
            this.publicJwkSet = new JWKSet(this.rsaKey.toPublicJWK());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build RSA JWK", e);
        }
    }

    public RSAKey rsaKey() {
        return rsaKey;
    }

    public String keyId() {
        return rsaKey.getKeyID();
    }

    /** JSON representation of the public JWK set for the {@code /.well-known/jwks.json} endpoint. */
    public Map<String, Object> jwkSetJson() {
        return publicJwkSet.toJSONObject();
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate RSA keypair", e);
        }
    }

    private static RSAPublicKey parsePublicKey(String pem) {
        try {
            byte[] der = Base64.getDecoder().decode(stripPem(pem));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid RSA public key PEM", e);
        }
    }

    private static RSAPrivateKey parsePrivateKey(String pem) {
        try {
            byte[] der = Base64.getDecoder().decode(stripPem(pem));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid RSA private key PEM (expected PKCS#8)", e);
        }
    }

    private static String stripPem(String pem) {
        return pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
