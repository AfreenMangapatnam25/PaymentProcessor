package com.paymentprocessor.fraudservice.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.fraudservice.document.ListEntry;
import com.paymentprocessor.fraudservice.service.ListEntryService;

@RestController
@RequestMapping("/api/lists")
public class ListEntryController {

    private final ListEntryService service;

    public ListEntryController(ListEntryService service) {
        this.service = service;
    }

    @GetMapping
    public List<ListEntry> all() {
        return service.findAll();
    }

    @PostMapping
    public ListEntry create(@RequestBody ListEntry entity) {
        return service.save(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListEntry> get(@PathVariable String id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ListEntry update(@PathVariable String id, @RequestBody ListEntry entity) {
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
