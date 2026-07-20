package com.paymentprocessor.authenticationservice.mfa;

import com.paymentprocessor.authenticationservice.config.AuthProperties;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.ByteBuffer;

/**
 * RFC 6238 (TOTP) / RFC 4226 (HOTP) implementation used for authenticator-app MFA.
 */
@Service
public class TotpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base32 BASE32 = new Base32();
    private static final int DIGITS = 6;
    private static final int SECRET_BYTES = 20; // 160-bit shared secret

    private final AuthProperties props;

    public TotpService(AuthProperties props) {
        this.props = props;
    }

    /** Generates a new Base32-encoded shared secret (no padding). */
    public String generateSecret() {
        byte[] buf = new byte[SECRET_BYTES];
        RANDOM.nextBytes(buf);
        return BASE32.encodeAsString(buf).replace("=", "");
    }

    /** Builds an otpauth:// provisioning URI for QR-code enrollment. */
    public String provisioningUri(String secret, String accountName) {
        String issuer = props.getMfa().getIssuerLabel();
        String label = enc(issuer) + ":" + enc(accountName);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + enc(issuer)
                + "&algorithm=SHA1&digits=" + DIGITS
                + "&period=" + props.getMfa().getTotpStepSeconds();
    }

    /** Verifies a submitted code against the secret, allowing +/- window steps for clock drift. */
    public boolean verify(String secret, String code) {
        if (code == null || !code.matches("\\d{" + DIGITS + "}")) {
            return false;
        }
        long step = props.getMfa().getTotpStepSeconds();
        long counter = System.currentTimeMillis() / 1000L / step;
        int window = props.getMfa().getTotpWindow();
        byte[] key = BASE32.decode(padBase32(secret));
        for (int i = -window; i <= window; i++) {
            String candidate = hotp(key, counter + i);
            if (constantTimeEquals(candidate, code)) {
                return true;
            }
        }
        return false;
    }

    private static String hotp(byte[] key, long counter) {
        try {
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("HOTP computation failed", e);
        }
    }

    private static String padBase32(String secret) {
        int mod = secret.length() % 8;
        if (mod == 0) return secret;
        return secret + "========".substring(mod);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
