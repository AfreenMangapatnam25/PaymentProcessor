package com.paymentprocessor.ledgerservice.controller;

import com.paymentprocessor.ledgerservice.service.BalanceSnapshotService;
import com.paymentprocessor.ledgerservice.web.dto.GenerateSnapshotRequest;
import com.paymentprocessor.ledgerservice.web.dto.SnapshotResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Balance Snapshots", description = "Immutable point-in-time balance captures")
public class BalanceSnapshotController {

    private final BalanceSnapshotService snapshotService;

    public BalanceSnapshotController(BalanceSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @PostMapping("/api/v1/snapshots")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate balance snapshots as of a date (all accounts or a subset)")
    public List<SnapshotResponse> generate(@Valid @RequestBody GenerateSnapshotRequest request) {
        return snapshotService.generate(request).stream()
                .map(BalanceSnapshotService::toResponse).toList();
    }

    @GetMapping("/api/v1/snapshots")
    @Operation(summary = "List snapshots for a given date")
    public List<SnapshotResponse> byDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return snapshotService.forDate(date).stream()
                .map(BalanceSnapshotService::toResponse).toList();
    }

    @GetMapping("/api/v1/accounts/{accountId}/snapshots")
    @Operation(summary = "List snapshots for an account")
    public List<SnapshotResponse> byAccount(@PathVariable String accountId) {
        return snapshotService.forAccount(accountId).stream()
                .map(BalanceSnapshotService::toResponse).toList();
    }
}
