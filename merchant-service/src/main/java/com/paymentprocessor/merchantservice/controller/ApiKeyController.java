package com.paymentprocessor.merchantservice.controller;

import com.paymentprocessor.merchantservice.dto.ApiKeyCreateRequest;
import com.paymentprocessor.merchantservice.dto.ApiKeyCreatedResponse;
import com.paymentprocessor.merchantservice.dto.ApiKeyResponse;
import com.paymentprocessor.merchantservice.service.ApiKeyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "API Keys")
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/api-keys")
public class ApiKeyController {

    private final ApiKeyService service;

    public ApiKeyController(ApiKeyService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApiKeyResponse> list(@PathVariable UUID merchantId) {
        return service.listActive(merchantId);
    }

    @PostMapping
    public ResponseEntity<ApiKeyCreatedResponse> generate(@PathVariable UUID merchantId,
                                                          @Valid @RequestBody ApiKeyCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generate(merchantId, req));
    }

    @PostMapping("/{keyId}/rotate")
    public ApiKeyCreatedResponse rotate(@PathVariable UUID merchantId, @PathVariable UUID keyId) {
        return service.rotate(merchantId, keyId);
    }

    @PutMapping("/{keyId}/ip-allowlist")
    public ApiKeyResponse configureIpAllowlist(@PathVariable UUID merchantId, @PathVariable UUID keyId,
                                               @RequestParam(required = false) @Size(max = 1024) String ipAllowlist) {
        return service.configureIpAllowlist(merchantId, keyId, ipAllowlist);
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID merchantId, @PathVariable UUID keyId) {
        service.revoke(merchantId, keyId);
        return ResponseEntity.noContent().build();
    }
}
