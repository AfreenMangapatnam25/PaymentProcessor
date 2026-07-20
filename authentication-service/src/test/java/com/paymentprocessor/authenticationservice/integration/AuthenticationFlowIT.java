package com.paymentprocessor.authenticationservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import com.paymentprocessor.authenticationservice.dto.RegisterIdentityRequest;
import com.paymentprocessor.authenticationservice.event.DomainEventPublisher;
import com.paymentprocessor.authenticationservice.service.IdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationFlowIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired IdentityService identityService;

    @MockBean DomainEventPublisher eventPublisher;

    private static final String PASSWORD = "Sup3rSecret!!";

    private void register(String email, boolean mfa) {
        identityService.register(new RegisterIdentityRequest(
                PrincipalType.USER, email, null, PASSWORD, mfa));
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    @Test
    void loginSucceedsAndReturnsTokens() throws Exception {
        register("success@example.com", false);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "success@example.com",
                                "password", PASSWORD, "deviceFingerprint", "fp-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("AUTHENTICATED")))
                .andExpect(jsonPath("$.tokens.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokens.refreshToken", notNullValue()));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        register("wrong@example.com", false);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "wrong@example.com",
                                "password", "WrongPass123!!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mfaRequiredReturnsChallenge() throws Exception {
        register("mfa@example.com", true);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "mfa@example.com",
                                "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("MFA_REQUIRED")))
                .andExpect(jsonPath("$.challenge.mfaToken", notNullValue()));
    }

    @Test
    void validationErrorReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "not-an-email", "password", ""))))
                .andExpect(status().isBadRequest());
    }
}
