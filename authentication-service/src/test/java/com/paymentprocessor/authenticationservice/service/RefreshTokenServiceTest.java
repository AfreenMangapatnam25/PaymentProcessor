package com.paymentprocessor.authenticationservice.service;

import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.entity.RefreshToken;
import com.paymentprocessor.authenticationservice.exception.UnauthorizedException;
import com.paymentprocessor.authenticationservice.repository.RefreshTokenRepository;
import com.paymentprocessor.authenticationservice.security.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private RefreshTokenRepository repository;
    private TokenHasher hasher;
    private RefreshTokenService service;

    @BeforeEach
    void setup() {
        repository = mock(RefreshTokenRepository.class);
        hasher = new TokenHasher();
        AuthProperties props = new AuthProperties();
        service = new RefreshTokenService(repository, hasher, props);
        when(repository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void issuesRawTokenAndPersistsHash() {
        RefreshTokenService.Issued issued = service.issue("identity-1", null, "device-1");
        assertThat(issued.rawToken()).isNotBlank();
        assertThat(issued.familyId()).isNotBlank();
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void reuseOfRevokedTokenRevokesFamily() {
        String raw = "raw-token-value";
        RefreshToken revoked = new RefreshToken();
        revoked.setId("t1");
        revoked.setIdentityId("identity-1");
        revoked.setFamilyId("fam-1");
        revoked.setTokenHash(hasher.sha256Hex(raw));
        revoked.setIssuedAt(Instant.now().minusSeconds(60));
        revoked.setExpiresAt(Instant.now().plusSeconds(600));
        revoked.setRevokedAt(Instant.now().minusSeconds(30));
        when(repository.findByTokenHash(hasher.sha256Hex(raw))).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.rotate(raw)).isInstanceOf(UnauthorizedException.class);
        verify(repository).revokeFamily(eq("fam-1"), any(Instant.class));
    }

    @Test
    void rotateIssuesNewAndRevokesOld() {
        String raw = "valid-token";
        RefreshToken active = new RefreshToken();
        active.setId("t2");
        active.setIdentityId("identity-2");
        active.setFamilyId("fam-2");
        active.setTokenHash(hasher.sha256Hex(raw));
        active.setIssuedAt(Instant.now().minusSeconds(10));
        active.setExpiresAt(Instant.now().plusSeconds(600));
        when(repository.findByTokenHash(hasher.sha256Hex(raw))).thenReturn(Optional.of(active));

        RefreshTokenService.Rotation rotation = service.rotate(raw);
        assertThat(rotation.identityId()).isEqualTo("identity-2");
        assertThat(rotation.familyId()).isEqualTo("fam-2");
        assertThat(rotation.rawToken()).isNotBlank();
        assertThat(active.getRevokedAt()).isNotNull();
    }
}
