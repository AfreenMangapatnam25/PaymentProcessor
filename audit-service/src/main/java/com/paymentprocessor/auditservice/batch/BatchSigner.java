package com.paymentprocessor.auditservice.batch;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.paymentprocessor.auditservice.config.AuditProperties;

/**
 * Signs the daily batch root with an EC (P-256) private key so the legal copy is not
 * only immutable (S3 Object Lock) but provably issued by this service. The signature
 * covers the root hash plus the sealed range and date, binding all three together.
 *
 * <p>The key is injected as a base64 PKCS#8 string sourced from a secrets manager. When
 * no key is configured (local/dev only), signing is skipped and an empty signature is
 * returned — never do this in production.
 */
@Component
public class BatchSigner {

    private static final Logger log = LoggerFactory.getLogger(BatchSigner.class);

    private final PrivateKey privateKey;
    private final String keyId;

    public BatchSigner(AuditProperties props) {
        this.keyId = props.getSigning().getKeyId();
        this.privateKey = loadKey(props.getSigning().getPrivateKeyPkcs8());
        if (privateKey == null) {
            log.warn("No batch signing key configured — daily batches will be UNSIGNED. "
                    + "Configure audit.signing.private-key-pkcs8 in any real environment.");
        }
    }

    public boolean isEnabled() {
        return privateKey != null;
    }

    public String getKeyId() {
        return keyId;
    }

    /**
     * Returns a base64 signature over the canonical signing payload, or empty when no
     * key is configured.
     */
    public String sign(String signingPayload) {
        if (privateKey == null) {
            return "";
        }
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(signingPayload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign batch root", e);
        }
    }

    private PrivateKey loadKey(String base64Pkcs8) {
        if (!StringUtils.hasText(base64Pkcs8)) {
            return null;
        }
        try {
            byte[] der = Base64.getDecoder().decode(base64Pkcs8.trim());
            KeyFactory kf = KeyFactory.getInstance("EC");
            return kf.generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid audit.signing.private-key-pkcs8", e);
        }
    }
}
