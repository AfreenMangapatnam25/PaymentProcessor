package com.paymentprocessor.ledgerservice.controller;

import com.paymentprocessor.ledgerservice.entity.Currency;
import com.paymentprocessor.ledgerservice.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/currencies")
@Tag(name = "Reference Data", description = "Account types and currencies")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping
    @Operation(summary = "List supported currencies")
    public List<Currency> list() {
        return currencyService.findAll();
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get a currency by code")
    public Currency get(@PathVariable String code) {
        return currencyService.getByCode(code);
    }
}
