package com.paymentprocessor.fraudservice.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.fraudservice.document.FraudCase;
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
    public ResponseEntity<FraudCase> get(@PathVariable String id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public FraudCase update(@PathVariable String id, @RequestBody FraudCase entity) {
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
