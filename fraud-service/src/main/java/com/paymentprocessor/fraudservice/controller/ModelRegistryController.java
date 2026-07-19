package com.paymentprocessor.fraudservice.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.fraudservice.document.ModelRegistry;
import com.paymentprocessor.fraudservice.service.ModelRegistryService;

@RestController
@RequestMapping("/api/model-registry")
public class ModelRegistryController {

    private final ModelRegistryService service;

    public ModelRegistryController(ModelRegistryService service) {
        this.service = service;
    }

    @GetMapping
    public List<ModelRegistry> all() {
        return service.findAll();
    }

    @PostMapping
    public ModelRegistry create(@RequestBody ModelRegistry entity) {
        return service.save(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModelRegistry> get(@PathVariable String id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ModelRegistry update(@PathVariable String id, @RequestBody ModelRegistry entity) {
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
