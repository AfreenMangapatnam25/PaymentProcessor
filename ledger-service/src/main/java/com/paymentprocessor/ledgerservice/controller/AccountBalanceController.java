package com.paymentprocessor.ledgerservice.controller;

import com.paymentprocessor.ledgerservice.service.AccountBalanceService;
import com.paymentprocessor.ledgerservice.web.dto.BalanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Balances", description = "Real-time account balances")
public class AccountBalanceController {

    private final AccountBalanceService balanceService;

    public AccountBalanceController(AccountBalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/api/v1/accounts/{accountId}/balance")
    @Operation(summary = "Get the real-time balance of an account")
    public BalanceResponse byAccount(@PathVariable String accountId) {
        return balanceService.toResponse(balanceService.get(accountId));
    }

    @GetMapping("/api/v1/balances/{accountId}")
    @Operation(summary = "Get the real-time balance of an account (alias)")
    public BalanceResponse get(@PathVariable String accountId) {
        return balanceService.toResponse(balanceService.get(accountId));
    }
}
