package com.paymentprocessor.tokenization.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.tokenization.entity.NetworkToken;
import com.paymentprocessor.tokenization.service.NetworkTokenService;

@RestController
@RequestMapping("/api/network-tokens")
public class NetworkTokenController {

    private final NetworkTokenService service;

    public NetworkTokenController(NetworkTokenService service) {
        this.service = service;
    }

    @GetMapping
    public List<NetworkToken> all() {
        return service.findAll();
    }

    @PostMapping
    public NetworkToken create(@RequestBody NetworkToken entity) {
        return service.save(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NetworkToken> get(@PathVariable String id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public NetworkToken update(@PathVariable String id, @RequestBody NetworkToken entity) {
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
