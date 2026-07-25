package com.paymentprocessor.authenticationservice.security;

import com.paymentprocessor.authenticationservice.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RsaKeyProviderTest {

    private static AuthProperties propsWithStore(Path keyFile) {
        AuthProperties props = new AuthProperties();
        props.getJwt().setKeyStorePath(keyFile.toString());
        return props;
    }

    @Test
    void generatesAndPersistsKeyOnFirstRun(@TempDir Path dir) {
        Path keyFile = dir.resolve("nested/jwt-signing-key.json");

        RsaKeyProvider provider = new RsaKeyProvider(propsWithStore(keyFile));

        assertThat(Files.exists(keyFile)).isTrue();
        assertThat(provider.keyId()).isNotBlank();
    }

    @Test
    void reusesSameKeyAcrossRestarts(@TempDir Path dir) {
        Path keyFile = dir.resolve("jwt-signing-key.json");
        AuthProperties props = propsWithStore(keyFile);

        String kidBeforeRestart = new RsaKeyProvider(props).keyId();
        // A second provider models the service restarting: it must reload, not regenerate.
        RsaKeyProvider afterRestart = new RsaKeyProvider(props);

        assertThat(afterRestart.keyId()).isEqualTo(kidBeforeRestart);
    }

    @Test
    void tokenSignedBeforeRestartStillVerifiesAfterRestart(@TempDir Path dir) throws Exception {
        Path keyFile = dir.resolve("jwt-signing-key.json");
        AuthProperties props = propsWithStore(keyFile);
        props.getJwt().setIssuer("https://auth.test.local");
        props.getJwt().setAudience("payment-platform-test");

        JwtService before = new JwtService(new RsaKeyProvider(props), props);
        String token = before.issueAccessToken(
                "id-1", com.paymentprocessor.authenticationservice.domain.PrincipalType.USER,
                java.util.List.of("user:self"), "sid-1", java.util.List.of("pwd")).value();

        // Restart: fresh provider reloads the persisted key; the old token must remain valid.
        JwtService after = new JwtService(new RsaKeyProvider(props), props);
        assertThat(after.verify(token).getSubject()).isEqualTo("id-1");
    }

    @Test
    void ephemeralWhenNoStoreConfigured(@TempDir Path dir) {
        // Two providers with no key-store path generate independent keys.
        String kid1 = new RsaKeyProvider(new AuthProperties()).keyId();
        String kid2 = new RsaKeyProvider(new AuthProperties()).keyId();
        assertThat(kid1).isNotEqualTo(kid2);
    }
}
