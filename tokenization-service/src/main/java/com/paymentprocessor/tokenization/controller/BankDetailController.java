package com.paymentprocessor.tokenization.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.tokenization.entity.BankDetail;
import com.paymentprocessor.tokenization.service.BankDetailService;

@RestController
@RequestMapping("/api/bank-details")
public class BankDetailController {

    private final BankDetailService service;

    public BankDetailController(BankDetailService service) {
        this.service = service;
    }

    @GetMapping
    public List<BankDetail> all() {
        return service.findAll();
    }

    @PostMapping
    public BankDetail create(@RequestBody BankDetail entity) {
        return service.save(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankDetail> get(@PathVariable String id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public BankDetail update(@PathVariable String id, @RequestBody BankDetail entity) {
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
