package com.paymentprocessor.fraudservice.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.fraudservice.domain.entity.FraudCase;
import com.paymentprocessor.fraudservice.service.FraudCaseService;

@RestController
@RequestMapping("/api/cases")
public class FraudCaseController {

    private final FraudCaseService service;

    public FraudCaseController(FraudCaseService service) {
        this.service = service;
    }

    @GetMapping
    public List<FraudCase> all() {
        return service.findAll();
    }

    @PostMapping
    public FraudCase create(@RequestBody FraudCase entity) {
        return service.save(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FraudCase> get(@PathVariable UUID id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public FraudCase update(@PathVariable UUID id, @RequestBody FraudCase entity) {
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
