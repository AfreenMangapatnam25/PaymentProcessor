package com.paymentprocessor.authenticationservice.controller;

import com.paymentprocessor.authenticationservice.domain.IdentityStatus;
import com.paymentprocessor.authenticationservice.dto.IdentityResponse;
import com.paymentprocessor.authenticationservice.dto.RegisterIdentityRequest;
import com.paymentprocessor.authenticationservice.entity.Identity;
import com.paymentprocessor.authenticationservice.service.IdentityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/identities")
public class AdminIdentityController {

    private final IdentityService identityService;

    public AdminIdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdentityResponse register(@Valid @RequestBody RegisterIdentityRequest request) {
        Identity identity = identityService.register(request);
        return IdentityResponse.from(identity);
    }

    @GetMapping("/{id}")
    public IdentityResponse get(@PathVariable String id) {
        return IdentityResponse.from(identityService.getById(id));
    }

    @PostMapping("/{id}/lock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void lock(@PathVariable String id) {
        identityService.setStatus(id, IdentityStatus.LOCKED);
    }

    @PostMapping("/{id}/unlock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlock(@PathVariable String id) {
        identityService.setStatus(id, IdentityStatus.ACTIVE);
    }

    @PostMapping("/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable String id) {
        identityService.setStatus(id, IdentityStatus.DISABLED);
    }
}
