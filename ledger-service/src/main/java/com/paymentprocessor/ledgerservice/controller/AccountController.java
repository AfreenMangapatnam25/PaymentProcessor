package com.paymentprocessor.ledgerservice.controller;

import com.paymentprocessor.ledgerservice.entity.Account;
import com.paymentprocessor.ledgerservice.service.AccountService;
import com.paymentprocessor.ledgerservice.web.dto.AccountResponse;
import com.paymentprocessor.ledgerservice.web.dto.CreateAccountRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Chart of accounts management")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a ledger account")
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.toResponse(accountService.create(request));
    }

    @GetMapping
    @Operation(summary = "List all accounts")
    public List<AccountResponse> list() {
        return accountService.list().stream().map(accountService::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an account by id")
    public AccountResponse get(@PathVariable String id) {
        return accountService.toResponse(accountService.get(id));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate an account (it stops accepting new entries)")
    public ResponseEntity<AccountResponse> deactivate(@PathVariable String id) {
        Account account = accountService.deactivate(id);
        return ResponseEntity.ok(accountService.toResponse(account));
    }
}
