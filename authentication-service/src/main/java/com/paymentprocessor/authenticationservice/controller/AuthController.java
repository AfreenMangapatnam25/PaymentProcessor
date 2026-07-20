package com.paymentprocessor.authenticationservice.controller;

import com.paymentprocessor.authenticationservice.dto.*;
import com.paymentprocessor.authenticationservice.security.AuthPrincipal;
import com.paymentprocessor.authenticationservice.service.AuthenticationService;
import com.paymentprocessor.authenticationservice.web.HttpRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authService;

    public AuthController(AuthenticationService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, HttpRequestUtils.context(http));
    }

    @PostMapping("/login/mfa")
    public TokenResponse completeMfa(@Valid @RequestBody MfaLoginRequest request, HttpServletRequest http) {
        return authService.completeMfaLogin(request, HttpRequestUtils.context(http));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal AuthPrincipal principal) {
        authService.logoutAll(principal.identityId());
    }

    @PostMapping("/token/introspect")
    public IntrospectResponse introspect(@Valid @RequestBody IntrospectRequest request) {
        return authService.introspect(request.token());
    }
}
