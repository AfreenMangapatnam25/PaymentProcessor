package com.paymentprocessor.authenticationservice.controller;

import com.paymentprocessor.authenticationservice.dto.*;
import com.paymentprocessor.authenticationservice.security.AuthPrincipal;
import com.paymentprocessor.authenticationservice.service.AuthenticationService;
import com.paymentprocessor.authenticationservice.service.PasswordResetService;
import com.paymentprocessor.authenticationservice.service.RateLimiterService;
import com.paymentprocessor.authenticationservice.web.HttpRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/passwords")
public class PasswordController {

    private final AuthenticationService authService;
    private final PasswordResetService resetService;
    private final RateLimiterService rateLimiter;

    public PasswordController(AuthenticationService authService,
                              PasswordResetService resetService,
                              RateLimiterService rateLimiter) {
        this.authService = authService;
        this.resetService = resetService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void change(@AuthenticationPrincipal AuthPrincipal principal,
                       @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.identityId(), request.currentPassword(), request.newPassword());
    }

    @PostMapping("/forgot")
    public MessageResponse forgot(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        rateLimiter.checkPasswordReset(HttpRequestUtils.clientIp(http));
        resetService.requestReset(request.email());
        // Always return the same response to prevent account enumeration.
        return MessageResponse.of("If the email exists, a reset link has been sent");
    }

    @PostMapping("/reset")
    public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest request) {
        resetService.reset(request.token(), request.newPassword());
        return MessageResponse.of("Password has been reset");
    }
}
