package com.paymentprocessor.authenticationservice.controller;

import com.paymentprocessor.authenticationservice.domain.VerificationChannel;
import com.paymentprocessor.authenticationservice.dto.ConfirmVerificationRequest;
import com.paymentprocessor.authenticationservice.dto.MessageResponse;
import com.paymentprocessor.authenticationservice.security.AuthPrincipal;
import com.paymentprocessor.authenticationservice.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/verification")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/email/start")
    public MessageResponse startEmail(@AuthenticationPrincipal AuthPrincipal principal) {
        verificationService.start(principal.identityId(), VerificationChannel.EMAIL);
        return MessageResponse.of("Verification email dispatched");
    }

    @PostMapping("/phone/start")
    public MessageResponse startPhone(@AuthenticationPrincipal AuthPrincipal principal) {
        verificationService.start(principal.identityId(), VerificationChannel.PHONE);
        return MessageResponse.of("Verification code dispatched");
    }

    @PostMapping("/email/confirm")
    public MessageResponse confirmEmail(@Valid @RequestBody ConfirmVerificationRequest request) {
        verificationService.confirm(request.token(), request.code());
        return MessageResponse.of("Email verified");
    }

    @PostMapping("/phone/confirm")
    public MessageResponse confirmPhone(@Valid @RequestBody ConfirmVerificationRequest request) {
        verificationService.confirm(request.token(), request.code());
        return MessageResponse.of("Phone verified");
    }
}
