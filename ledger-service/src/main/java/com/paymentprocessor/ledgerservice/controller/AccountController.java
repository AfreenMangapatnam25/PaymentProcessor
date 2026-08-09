package com.paymentprocessor.ledgerservice.controller;

import com.paymentprocessor.ledgerservice.domain.enums.AccountPurpose;
import com.paymentprocessor.ledgerservice.entity.Account;
import com.paymentprocessor.ledgerservice.service.AccountService;
import com.paymentprocessor.ledgerservice.web.dto.AccountResponse;
import com.paymentprocessor.ledgerservice.web.dto.CreateAccountRequest;
import com.paymentprocessor.ledgerservice.web.dto.ProvisionMerchantAccountsRequest;
import com.paymentprocessor.ledgerservice.web.dto.ProvisionMerchantAccountsResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    @Operation(summary = "List accounts, optionally filtered by owner or account code")
    public List<AccountResponse> list(@RequestParam(required = false) String ownerType,
                                      @RequestParam(required = false) String ownerId,
                                      @RequestParam(required = false) String accountCode) {
        return accountService.list(ownerType, ownerId, accountCode).stream()
                .map(accountService::toResponse).toList();
    }

    @GetMapping("/resolve")
    @Operation(summary = "Resolve a well-known account purpose to an account id")
    public AccountResponse resolve(@RequestParam AccountPurpose purpose,
                                   @RequestParam(required = false) String ownerId,
                                   @RequestParam(defaultValue = "USD") String currency) {
        return accountService.toResponse(accountService.resolve(purpose, ownerId, currency));
    }

    @PostMapping("/provision-merchant")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Provision merchant settlement and reserve ledger accounts (idempotent)")
    public ProvisionMerchantAccountsResponse provisionMerchant(
            @Valid @RequestBody ProvisionMerchantAccountsRequest request) {
        return accountService.provisionMerchantAccounts(request);
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
