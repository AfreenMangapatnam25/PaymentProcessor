package com.paymentprocessor.merchantservice.common;

import com.paymentprocessor.merchantservice.common.crypto.EncryptionService;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionServiceTest {

    private final EncryptionService service =
            new EncryptionService(Base64.getEncoder().encodeToString(new byte[32]));

    @Test
    void roundTripsPlaintext() {
        String plaintext = "GB33BUKB20201555555555";
        String encrypted = service.encrypt(plaintext);
        assertThat(encrypted).isNotNull().isNotEqualTo(plaintext);
        assertThat(service.decrypt(encrypted)).isEqualTo(plaintext);
    }

    @Test
    void producesDistinctCiphertextsForSameInput() {
        String enc1 = service.encrypt("4242424242424242");
        String enc2 = service.encrypt("4242424242424242");
        assertThat(enc1).isNotEqualTo(enc2);           // random IV per encryption
        assertThat(service.decrypt(enc1)).isEqualTo(service.decrypt(enc2));
    }

    @Test
    void handlesNulls() {
        assertThat(service.encrypt(null)).isNull();
        assertThat(service.decrypt(null)).isNull();
    }
}
