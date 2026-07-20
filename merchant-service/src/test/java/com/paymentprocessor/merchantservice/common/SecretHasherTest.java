package com.paymentprocessor.merchantservice.common;

import com.paymentprocessor.merchantservice.common.crypto.SecretHasher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretHasherTest {

    private final SecretHasher hasher = new SecretHasher();

    @Test
    void matchesCorrectSecret() {
        String secret = hasher.generateSecret(32);
        String hash = hasher.hash(secret);
        assertThat(hasher.matches(secret, hash)).isTrue();
    }

    @Test
    void rejectsWrongSecret() {
        String hash = hasher.hash(hasher.generateSecret(32));
        assertThat(hasher.matches("not-the-secret", hash)).isFalse();
        assertThat(hasher.matches(null, hash)).isFalse();
    }

    @Test
    void generatesDistinctSecrets() {
        assertThat(hasher.generateSecret(32)).isNotEqualTo(hasher.generateSecret(32));
    }
}
