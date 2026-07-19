package com.paymentprocessor.authorization.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code security.jwt.*} configuration namespace used to validate identity tokens
 * issued by the Authentication Service.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /** When false, the claims filter decodes tokens without signature verification (local/dev only). */
    private boolean verifySignature = true;

    /**
     * HMAC (HS256/384/512) shared secret, base64 or raw. Used when {@link #verifySignature} is true
     * and no public key is configured.
     */
    private String hmacSecret;

    /** PEM-encoded RSA/EC public key (contents, not a path) for asymmetric verification. */
    private String publicKey;

    /** Expected issuer claim; when set, tokens with a different {@code iss} are rejected. */
    private String issuer;

    /** Header carrying the bearer token. */
    private String header = "Authorization";

    /** Claim holding the subject/identity id. */
    private String subjectClaim = "sub";

    /** Claim holding the granted roles (array or space-delimited string). */
    private String rolesClaim = "roles";

    /** Claim holding the OAuth2 scopes (array or space-delimited string). */
    private String scopeClaim = "scope";

    /** Claim holding the merchant id for merchant-scoped identities. */
    private String merchantClaim = "merchant_id";
}
