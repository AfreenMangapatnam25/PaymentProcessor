package com.paymentprocessor.authenticationservice.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Issues and verifies RS256 JWTs. Two token purposes exist:
 * <ul>
 *   <li>{@code access} - full access token carrying scopes.</li>
 *   <li>{@code mfa} - short-lived ticket proving password success while a second
 *       factor is still pending; it carries no scopes and is rejected by the
 *       resource filter.</li>
 * </ul>
 * Downstream services (gateway) verify independently using the published JWK set.
 */
@Service
public class JwtService {

    public static final String PURPOSE_ACCESS = "access";
    public static final String PURPOSE_MFA = "mfa";
    private static final Duration MFA_TICKET_TTL = Duration.ofMinutes(5);

    private final RsaKeyProvider keyProvider;
    private final AuthProperties props;
    private final RSASSASigner signer;
    private final RSASSAVerifier verifier;

    public JwtService(RsaKeyProvider keyProvider, AuthProperties props) throws JOSEException {
        this.keyProvider = keyProvider;
        this.props = props;
        this.signer = new RSASSASigner(keyProvider.rsaKey());
        this.verifier = new RSASSAVerifier(keyProvider.rsaKey().toRSAPublicKey());
    }

    public record IssuedToken(String value, Instant expiresAt, String jti) {}

    public IssuedToken issueAccessToken(String identityId, PrincipalType principalType,
                                        List<String> scopes, String sessionId, List<String> amr) {
        JWTClaimsSet claims = baseBuilder(identityId, principalType, props.getJwt().getAccessTokenTtl())
                .claim("purpose", PURPOSE_ACCESS)
                .claim("scope", scopes == null ? "" : String.join(" ", scopes))
                .claim("sid", sessionId)
                .claim("amr", amr == null ? List.of() : amr)
                .build();
        return sign(claims);
    }

    public IssuedToken issueMfaTicket(String identityId, PrincipalType principalType) {
        JWTClaimsSet claims = baseBuilder(identityId, principalType, MFA_TICKET_TTL)
                .claim("purpose", PURPOSE_MFA)
                .build();
        return sign(claims);
    }

    private JWTClaimsSet.Builder baseBuilder(String identityId, PrincipalType principalType, Duration ttl) {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(props.getJwt().getIssuer())
                .audience(props.getJwt().getAudience())
                .subject(identityId)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ttl)))
                .claim("identity_id", identityId)
                .claim("principal_type", principalType.name());
    }

    private IssuedToken sign(JWTClaimsSet claims) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(keyProvider.keyId())
                    .type(JOSEObjectType.JWT)
                    .build();
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            return new IssuedToken(jwt.serialize(), claims.getExpirationTime().toInstant(),
                    claims.getJWTID());
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    /** Verifies signature, expiry, issuer and audience. Returns claims or throws. */
    public JWTClaimsSet verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(verifier)) {
                throw new JwtValidationException("Invalid token signature");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Instant now = Instant.now();
            if (claims.getExpirationTime() == null || claims.getExpirationTime().toInstant().isBefore(now)) {
                throw new JwtValidationException("Token expired");
            }
            if (!props.getJwt().getIssuer().equals(claims.getIssuer())) {
                throw new JwtValidationException("Unexpected issuer");
            }
            if (claims.getAudience() == null || !claims.getAudience().contains(props.getJwt().getAudience())) {
                throw new JwtValidationException("Unexpected audience");
            }
            return claims;
        } catch (JwtValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtValidationException("Malformed token");
        }
    }

    public String purpose(JWTClaimsSet claims) {
        try {
            Object p = claims.getClaim("purpose");
            return p == null ? null : p.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static class JwtValidationException extends RuntimeException {
        public JwtValidationException(String message) { super(message); }
    }
}
