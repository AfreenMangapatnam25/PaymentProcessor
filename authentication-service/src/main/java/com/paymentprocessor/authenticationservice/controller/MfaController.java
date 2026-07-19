package com.paymentprocessor.authenticationservice.controller;

import com.paymentprocessor.authenticationservice.dto.*;
import com.paymentprocessor.authenticationservice.entity.Identity;
import com.paymentprocessor.authenticationservice.security.AuthPrincipal;
import com.paymentprocessor.authenticationservice.service.IdentityService;
import com.paymentprocessor.authenticationservice.service.MfaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mfa")
public class MfaController {

    private final MfaService mfaService;
    private final IdentityService identityService;

    public MfaController(MfaService mfaService, IdentityService identityService) {
        this.mfaService = mfaService;
        this.identityService = identityService;
    }

    @PostMapping("/enroll")
    public MfaEnrollResponse enroll(@AuthenticationPrincipal AuthPrincipal principal,
                                    @RequestParam(required = false) String label) {
        Identity identity = identityService.getById(principal.identityId());
        String account = identity.getEmail() != null ? identity.getEmail() : identity.getId();
        return mfaService.enrollTotp(principal.identityId(), account, label);
    }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@AuthenticationPrincipal AuthPrincipal principal,
                       @Valid @RequestBody MfaVerifyRequest request) {
        mfaService.verifyEnrollment(principal.identityId(), request.factorId(), request.code());
    }

    @PostMapping("/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@AuthenticationPrincipal AuthPrincipal principal,
                        @Valid @RequestBody MfaDisableRequest request) {
        // Re-authenticate before disabling a factor.
        identityService.getById(principal.identityId());
        mfaService.disable(principal.identityId(), request.factorId());
    }

    @PostMapping("/recovery-codes")
    public RecoveryCodesResponse recoveryCodes(@AuthenticationPrincipal AuthPrincipal principal) {
        return new RecoveryCodesResponse(mfaService.regenerateRecoveryCodes(principal.identityId()));
    }

    @GetMapping
    public Map<String, List<String>> status(@AuthenticationPrincipal AuthPrincipal principal) {
        return Map.of("activeMethods", mfaService.activeMethods(principal.identityId()));
    }
}
