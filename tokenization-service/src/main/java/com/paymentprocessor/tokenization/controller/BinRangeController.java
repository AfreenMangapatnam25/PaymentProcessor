package com.paymentprocessor.tokenization.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.tokenization.entity.BinRange;
import com.paymentprocessor.tokenization.service.BinRangeService;

@RestController
@RequestMapping("/api/bin-ranges")
public class BinRangeController {

    private final BinRangeService service;

    public BinRangeController(BinRangeService service) {
        this.service = service;
    }

    @GetMapping
    public List<BinRange> all() {
        return service.findAll();
    }

    @PostMapping
    public BinRange create(@RequestBody BinRange entity) {
        return service.save(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BinRange> get(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public BinRange update(@PathVariable Long id, @RequestBody BinRange entity) {
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
