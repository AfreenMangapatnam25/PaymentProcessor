package com.paymentprocessor.notificationservice.service.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RecipientHasherTest {

    private final RecipientHasher hasher = new RecipientHasher();

    @Test
    void emailHashingIsCaseAndWhitespaceInsensitive() {
        byte[] a = hasher.hash("email", "  Someone@Example.com ");
        byte[] b = hasher.hash("email", "someone@example.com");

        assertThat(a).isEqualTo(b);
    }

    @Test
    void phoneHashingIgnoresFormatting() {
        byte[] a = hasher.hash("sms", "+1 (555) 123-4567");
        byte[] b = hasher.hash("sms", "+15551234567");

        assertThat(a).isEqualTo(b);
    }

    @Test
    void sameRecipientOnDifferentChannelsHashesDifferently() {
        byte[] email = hasher.hash("email", "shared@example.com");
        byte[] sms = hasher.hash("sms", "shared@example.com");

        assertThat(email).isNotEqualTo(sms);
    }

    @Test
    void differentRecipientsHashDifferently() {
        byte[] a = hasher.hash("email", "a@example.com");
        byte[] b = hasher.hash("email", "b@example.com");

        assertThat(a).isNotEqualTo(b);
    }
}
