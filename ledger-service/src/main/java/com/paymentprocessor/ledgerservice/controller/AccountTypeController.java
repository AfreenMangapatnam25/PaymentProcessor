package com.paymentprocessor.ledgerservice.controller;

import com.paymentprocessor.ledgerservice.entity.AccountType;
import com.paymentprocessor.ledgerservice.service.AccountTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account-types")
@Tag(name = "Reference Data", description = "Account types and currencies")
public class AccountTypeController {

    private final AccountTypeService accountTypeService;

    public AccountTypeController(AccountTypeService accountTypeService) {
        this.accountTypeService = accountTypeService;
    }

    @GetMapping
    @Operation(summary = "List account types")
    public List<AccountType> list() {
        return accountTypeService.findAll();
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get an account type by code")
    public AccountType get(@PathVariable String code) {
        return accountTypeService.getByCode(code);
    }
}
