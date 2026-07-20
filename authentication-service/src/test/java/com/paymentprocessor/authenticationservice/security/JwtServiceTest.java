package com.paymentprocessor.authenticationservice.security;

import com.nimbusds.jwt.JWTClaimsSet;
import com.paymentprocessor.authenticationservice.config.AuthProperties;
import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setup() throws Exception {
        AuthProperties props = new AuthProperties();
        props.getJwt().setIssuer("https://auth.test.local");
        props.getJwt().setAudience("payment-platform-test");
        RsaKeyProvider keyProvider = new RsaKeyProvider(props);
        jwtService = new JwtService(keyProvider, props);
    }

    @Test
    void issuesAndVerifiesAccessToken() {
        JwtService.IssuedToken token = jwtService.issueAccessToken(
                "id-123", PrincipalType.USER, List.of("user:self"), "sid-1", List.of("pwd"));

        JWTClaimsSet claims = jwtService.verify(token.value());
        assertThat(claims.getSubject()).isEqualTo("id-123");
        assertThat(jwtService.purpose(claims)).isEqualTo(JwtService.PURPOSE_ACCESS);
        assertThat(claims.getClaim("scope")).isEqualTo("user:self");
        assertThat(claims.getClaim("principal_type")).isEqualTo("USER");
    }

    @Test
    void mfaTicketHasMfaPurpose() {
        JwtService.IssuedToken ticket = jwtService.issueMfaTicket("id-9", PrincipalType.ADMIN);
        JWTClaimsSet claims = jwtService.verify(ticket.value());
        assertThat(jwtService.purpose(claims)).isEqualTo(JwtService.PURPOSE_MFA);
    }

    @Test
    void rejectsTamperedToken() {
        JwtService.IssuedToken token = jwtService.issueAccessToken(
                "id-123", PrincipalType.USER, List.of(), "sid-1", List.of());
        String tampered = token.value().substring(0, token.value().length() - 3) + "aaa";
        assertThatThrownBy(() -> jwtService.verify(tampered))
                .isInstanceOf(JwtService.JwtValidationException.class);
    }
}
