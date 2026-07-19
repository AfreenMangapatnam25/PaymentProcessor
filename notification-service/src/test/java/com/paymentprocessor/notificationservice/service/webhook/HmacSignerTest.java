package com.paymentprocessor.notificationservice.service.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class HmacSignerTest {

    private final HmacSigner signer = new HmacSigner();

    @Test
    void signatureVerifiesWithTheSameSecretAndBody() {
        String secret = "whsec_test";
        String body = "{\"id\":\"evt_123\",\"type\":\"payment.succeeded\"}";
        Instant now = Instant.now();

        String header = signer.sign(secret, body, now);

        assertThat(header).startsWith("t=" + now.getEpochSecond() + ",v1=");
        assertThat(signer.verify(secret, body, header)).isTrue();
    }

    @Test
    void verificationFailsIfBodyIsTampered() {
        String secret = "whsec_test";
        String header = signer.sign(secret, "{\"amount\":100}", Instant.now());

        assertThat(signer.verify(secret, "{\"amount\":100000}", header)).isFalse();
    }

    @Test
    void verificationFailsWithTheWrongSecret() {
        String body = "{\"amount\":100}";
        String header = signer.sign("whsec_correct", body, Instant.now());

        assertThat(signer.verify("whsec_wrong", body, header)).isFalse();
    }

    @Test
    void verificationFailsOnMalformedHeader() {
        assertThat(signer.verify("secret", "body", "not-a-valid-header")).isFalse();
    }
}
