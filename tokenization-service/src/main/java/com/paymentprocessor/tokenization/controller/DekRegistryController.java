package com.paymentprocessor.tokenization.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.tokenization.entity.DekRegistry;
import com.paymentprocessor.tokenization.service.DekRegistryService;

@RestController
@RequestMapping("/api/dek-registry")
public class DekRegistryController {

    private final DekRegistryService service;

    public DekRegistryController(DekRegistryService service) {
        this.service = service;
    }

    @GetMapping
    public List<DekRegistry> all() {
        return service.findAll();
    }

    @PostMapping
    public DekRegistry create(@RequestBody DekRegistry entity) {
        return service.save(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DekRegistry> get(@PathVariable String id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public DekRegistry update(@PathVariable String id, @RequestBody DekRegistry entity) {
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
