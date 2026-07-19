package com.paymentprocessor.fraudservice.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.fraudservice.document.Rule;
import com.paymentprocessor.fraudservice.service.RuleService;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleService service;

    public RuleController(RuleService service) {
        this.service = service;
    }

    @GetMapping
    public List<Rule> all() {
        return service.findAll();
    }

    @PostMapping
    public Rule create(@RequestBody Rule entity) {
        return service.save(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Rule> get(@PathVariable String id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public Rule update(@PathVariable String id, @RequestBody Rule entity) {
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
