package com.paymentprocessor.tokenization.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.tokenization.entity.Instrument;
import com.paymentprocessor.tokenization.service.InstrumentService;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentService service;

    public InstrumentController(InstrumentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Instrument> all() {
        return service.findAll();
    }

    @PostMapping
    public Instrument create(@RequestBody Instrument entity) {
        return service.save(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Instrument> get(@PathVariable String id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public Instrument update(@PathVariable String id, @RequestBody Instrument entity) {
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
