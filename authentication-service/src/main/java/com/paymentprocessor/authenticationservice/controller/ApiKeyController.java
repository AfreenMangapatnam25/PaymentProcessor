package com.paymentprocessor.authenticationservice.controller;

import com.paymentprocessor.authenticationservice.domain.PrincipalType;
import com.paymentprocessor.authenticationservice.dto.*;
import com.paymentprocessor.authenticationservice.service.ApiKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    /** Admin-only: mint a new service API key. Plaintext secret is returned once. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyCreatedResponse create(@Valid @RequestBody ApiKeyCreateRequest request) {
        return apiKeyService.create(request);
    }

    @GetMapping
    public List<ApiKeyResponse> list(@RequestParam PrincipalType ownerType,
                                     @RequestParam String ownerId) {
        return apiKeyService.list(ownerType, ownerId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable String id) {
        apiKeyService.revoke(id);
    }

    /** Public service-to-service endpoint used by other microservices to validate a key. */
    @PostMapping("/verify")
    public ApiKeyVerifyResponse verify(@Valid @RequestBody ApiKeyVerifyRequest request) {
        return apiKeyService.verify(request.apiKey());
    }
}
