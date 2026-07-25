package com.paymentprocessor.authenticationservice.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.paymentprocessor.authenticationservice.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides the RSA signing key used for RS256 access tokens and exposes the
 * corresponding public JWK set consumed by the gateway-service.
 *
 * <p>Key material is resolved in priority order:</p>
 * <ol>
 *   <li><b>Configured PEM</b> ({@code JWT_PRIVATE_KEY} / {@code JWT_PUBLIC_KEY}) —
 *       the production path: inject the keypair from a secrets manager.</li>
 *   <li><b>Persisted JWK file</b> ({@code auth.jwt.key-store-path}, set by default in
 *       {@code application.yml}) — with no PEM configured, a 2048-bit keypair is
 *       generated on first run and written to this path, then reloaded on every
 *       subsequent start. Because the same key is reused across restarts, tokens
 *       already issued keep validating after a bounce. This keeps local/dev stable
 *       with no manual key handling, but a file on the app host is <i>not</i> a
 *       secrets manager — prefer the injected PEMs in production.</li>
 *   <li><b>Ephemeral</b> — if neither a PEM nor a key-store path is configured, a
 *       keypair is generated in memory and lost on restart (invalidating every
 *       issued token). Used only in tests / throwaway runs.</li>
 * </ol>
 */
@Component
public class RsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);

    private final RSAKey rsaKey;
    private final JWKSet publicJwkSet;

    public RsaKeyProvider(AuthProperties props) {
        AuthProperties.Jwt cfg = props.getJwt();
        try {
            if (hasText(cfg.getPrivateKeyPem()) && hasText(cfg.getPublicKeyPem())) {
                this.rsaKey = fromPem(cfg);
                log.info("Loaded RSA signing key from configured PEM material (kid={}).",
                        this.rsaKey.getKeyID());
            } else if (hasText(cfg.getKeyStorePath())) {
                this.rsaKey = loadOrCreatePersistentKey(cfg);
            } else {
                this.rsaKey = generateEphemeralKey(cfg);
                log.warn("No RSA key configured (JWT_PRIVATE_KEY/JWT_PUBLIC_KEY) and no key-store-path "
                        + "set. Generated an EPHEMERAL keypair — tokens will NOT survive a restart. "
                        + "Set auth.jwt.key-store-path (or supply PEMs) for a stable signing key.");
            }
            this.publicJwkSet = new JWKSet(this.rsaKey.toPublicJWK());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialise RSA signing key", e);
        }
    }

    private static RSAKey fromPem(AuthProperties.Jwt cfg) throws Exception {
        RSAPublicKey publicKey = parsePublicKey(cfg.getPublicKeyPem());
        RSAPrivateKey privateKey = parsePrivateKey(cfg.getPrivateKeyPem());
        return buildKey(publicKey, privateKey, cfg.getKeyId());
    }

    private static RSAKey generateEphemeralKey(AuthProperties.Jwt cfg) throws Exception {
        KeyPair kp = generateKeyPair();
        return buildKey((RSAPublicKey) kp.getPublic(), (RSAPrivateKey) kp.getPrivate(), cfg.getKeyId());
    }

    /**
     * Reloads the persisted signing key, or generates and persists one on first run.
     * Reusing the same key across restarts is what stops previously-issued tokens
     * from being rejected with 401 after the service bounces.
     */
    private static RSAKey loadOrCreatePersistentKey(AuthProperties.Jwt cfg) throws Exception {
        Path path = Path.of(cfg.getKeyStorePath());

        if (Files.exists(path)) {
            RSAKey loaded = RSAKey.parse(Files.readString(path, StandardCharsets.UTF_8));
            if (loaded.getPrivateExponent() == null) {
                throw new IllegalStateException(
                        "Key store at " + path.toAbsolutePath() + " has no private key");
            }
            log.info("Loaded persistent RSA signing key from {} (kid={}).",
                    path.toAbsolutePath(), loaded.getKeyID());
            return loaded;
        }

        KeyPair kp = generateKeyPair();
        RSAKey generated = buildKey((RSAPublicKey) kp.getPublic(),
                (RSAPrivateKey) kp.getPrivate(), cfg.getKeyId());
        persist(path, generated);
        log.warn("No RSA key configured (JWT_PRIVATE_KEY/JWT_PUBLIC_KEY). Generated a new keypair "
                + "and persisted it to {} (kid={}); it will now survive restarts. For production, "
                + "supply the keypair via JWT_PRIVATE_KEY/JWT_PUBLIC_KEY from a secrets manager.",
                path.toAbsolutePath(), generated.getKeyID());
        return generated;
    }

    private static RSAKey buildKey(RSAPublicKey publicKey, RSAPrivateKey privateKey, String configuredKid)
            throws Exception {
        RSAKey partial = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();
        String kid = hasText(configuredKid) ? configuredKid : partial.computeThumbprint().toString();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(kid)
                .build();
    }

    /** Writes the full (private-key-bearing) JWK to disk, owner-only where the OS supports it. */
    private static void persist(Path path, RSAKey key) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, key.toJSONString(), StandardCharsets.UTF_8);
        try {
            Set<PosixFilePermission> ownerOnly = EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(path, PosixFilePermissions.asFileAttribute(ownerOnly).value());
        } catch (UnsupportedOperationException | IOException ignored) {
            // Non-POSIX filesystem (e.g. Windows): file inherits directory ACLs.
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
