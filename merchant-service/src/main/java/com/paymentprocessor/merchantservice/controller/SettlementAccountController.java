package com.paymentprocessor.merchantservice.controller;

import com.paymentprocessor.merchantservice.dto.SettlementAccountRequest;
import com.paymentprocessor.merchantservice.dto.SettlementAccountResponse;
import com.paymentprocessor.merchantservice.service.SettlementAccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Settlement Accounts")
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/settlement-accounts")
public class SettlementAccountController {

    private final SettlementAccountService service;

    public SettlementAccountController(SettlementAccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<SettlementAccountResponse> list(@PathVariable UUID merchantId) {
        return service.list(merchantId);
    }

    @PostMapping
    public ResponseEntity<SettlementAccountResponse> add(@PathVariable UUID merchantId,
                                                         @Valid @RequestBody SettlementAccountRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(merchantId, req));
    }

    @PutMapping("/{accountId}")
    public SettlementAccountResponse update(@PathVariable UUID merchantId, @PathVariable UUID accountId,
                                            @Valid @RequestBody SettlementAccountRequest req) {
        return service.update(merchantId, accountId, req);
    }

    @PostMapping("/{accountId}/default")
    public SettlementAccountResponse markDefault(@PathVariable UUID merchantId, @PathVariable UUID accountId) {
        return service.markDefault(merchantId, accountId);
    }

    @PostMapping("/{accountId}/verify")
    public SettlementAccountResponse verify(@PathVariable UUID merchantId, @PathVariable UUID accountId) {
        return service.markVerified(merchantId, accountId);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> delete(@PathVariable UUID merchantId, @PathVariable UUID accountId) {
        service.delete(merchantId, accountId);
        return ResponseEntity.noContent().build();
    }
}
