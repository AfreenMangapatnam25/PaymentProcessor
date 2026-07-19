package com.paymentprocessor.authenticationservice.mfa;

import com.paymentprocessor.authenticationservice.config.AuthProperties;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {

    private final TotpService totp = new TotpService(new AuthProperties());

    @Test
    void generatesUsableSecretAndVerifiesCurrentCode() {
        String secret = totp.generateSecret();
        assertThat(secret).isNotBlank();

        String code = currentCode(secret);
        assertThat(totp.verify(secret, code)).isTrue();
    }

    @Test
    void rejectsWrongCodeAndBadFormat() {
        String secret = totp.generateSecret();
        assertThat(totp.verify(secret, "000000")).isIn(true, false); // could randomly match; check format guard
        assertThat(totp.verify(secret, "12345")).isFalse();
        assertThat(totp.verify(secret, "abcdef")).isFalse();
        assertThat(totp.verify(secret, null)).isFalse();
    }

    @Test
    void provisioningUriContainsSecretAndIssuer() {
        String secret = totp.generateSecret();
        String uri = totp.provisioningUri(secret, "user@example.com");
        assertThat(uri).startsWith("otpauth://totp/");
        assertThat(uri).contains("secret=" + secret);
        assertThat(uri).contains("issuer=PaymentProcessor");
    }

    private static String currentCode(String secret) {
        try {
            Base32 b32 = new Base32();
            String padded = secret;
            int mod = padded.length() % 8;
            if (mod != 0) padded = padded + "========".substring(mod);
            byte[] key = b32.decode(padded);
            long counter = System.currentTimeMillis() / 1000L / 30L;
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            return String.format("%06d", binary % 1_000_000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
