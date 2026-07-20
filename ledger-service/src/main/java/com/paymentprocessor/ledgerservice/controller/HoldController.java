package com.paymentprocessor.ledgerservice.controller;

import com.paymentprocessor.ledgerservice.service.HoldService;
import com.paymentprocessor.ledgerservice.web.dto.HoldResponse;
import com.paymentprocessor.ledgerservice.web.dto.PlaceHoldRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Holds", description = "Reservations against an account's available balance")
public class HoldController {

    private final HoldService holdService;

    public HoldController(HoldService holdService) {
        this.holdService = holdService;
    }

    @PostMapping("/api/v1/holds")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place a hold against available balance")
    public HoldResponse place(@Valid @RequestBody PlaceHoldRequest request) {
        return HoldService.toResponse(holdService.place(request));
    }

    @PostMapping("/api/v1/holds/{id}/release")
    @Operation(summary = "Release an active hold")
    public HoldResponse release(@PathVariable String id) {
        return HoldService.toResponse(holdService.release(id));
    }

    @GetMapping("/api/v1/holds/{id}")
    @Operation(summary = "Get a hold by id")
    public HoldResponse get(@PathVariable String id) {
        return HoldService.toResponse(holdService.get(id));
    }

    @GetMapping("/api/v1/accounts/{accountId}/holds")
    @Operation(summary = "List holds for an account")
    public List<HoldResponse> forAccount(@PathVariable String accountId) {
        return holdService.listForAccount(accountId).stream().map(HoldService::toResponse).toList();
    }
}
