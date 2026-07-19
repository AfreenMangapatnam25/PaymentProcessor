package com.paymentprocessor.limit.controller;

import com.paymentprocessor.limit.dto.LimitConfigRequest;
import com.paymentprocessor.limit.dto.LimitConfigResponse;
import com.paymentprocessor.limit.service.LimitConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Administrative CRUD for limit configurations.
 */
@RestController
@RequestMapping("/api/v1/limit-configs")
@RequiredArgsConstructor
@Tag(name = "Limit Configuration", description = "Manage limit rules and thresholds")
public class LimitConfigController {

    private final LimitConfigService configService;

    @PostMapping
    @Operation(summary = "Create a limit configuration")
    public ResponseEntity<LimitConfigResponse> create(@Valid @RequestBody LimitConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(configService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a limit configuration")
    public ResponseEntity<LimitConfigResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody LimitConfigRequest request) {
        return ResponseEntity.ok(configService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Disable (soft-delete) a limit configuration")
    public ResponseEntity<Void> disable(@PathVariable UUID id) {
        configService.disable(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a limit configuration")
    public ResponseEntity<LimitConfigResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(configService.get(id));
    }

    @GetMapping
    @Operation(summary = "List all limit configurations")
    public ResponseEntity<List<LimitConfigResponse>> list() {
        return ResponseEntity.ok(configService.listAll());
    }
}
