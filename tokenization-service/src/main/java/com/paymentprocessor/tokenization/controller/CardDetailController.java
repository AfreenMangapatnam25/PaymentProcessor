package com.paymentprocessor.tokenization.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.tokenization.entity.CardDetail;
import com.paymentprocessor.tokenization.service.CardDetailService;

@RestController
@RequestMapping("/api/card-details")
public class CardDetailController {

    private final CardDetailService service;

    public CardDetailController(CardDetailService service) {
        this.service = service;
    }

    @GetMapping
    public List<CardDetail> all() {
        return service.findAll();
    }

    @PostMapping
    public CardDetail create(@RequestBody CardDetail entity) {
        return service.save(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardDetail> get(@PathVariable String id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public CardDetail update(@PathVariable String id, @RequestBody CardDetail entity) {
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
