package com.paymentprocessor.ledgerservice.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.ledgerservice.entity.BalanceShard;
import com.paymentprocessor.ledgerservice.service.BalanceShardService;

@RestController
@RequestMapping("/api/balance-shards")
public class BalanceShardController {

    private final BalanceShardService service;

    public BalanceShardController(BalanceShardService service) {
        this.service = service;
    }

    @GetMapping
    public List<BalanceShard> all() {
        return service.findAll();
    }

    @PostMapping
    public BalanceShard create(@RequestBody BalanceShard entity) {
        return service.save(entity);
    }
}
