package com.paymentprocessor.tokenization.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.tokenization.entity.InstrumentAccessLog;
import com.paymentprocessor.tokenization.service.InstrumentAccessLogService;

@RestController
@RequestMapping("/api/instrument-access-log")
public class InstrumentAccessLogController {

    private final InstrumentAccessLogService service;

    public InstrumentAccessLogController(InstrumentAccessLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<InstrumentAccessLog> all() {
        return service.findAll();
    }

    @PostMapping
    public InstrumentAccessLog create(@RequestBody InstrumentAccessLog entity) {
        return service.save(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstrumentAccessLog> get(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public InstrumentAccessLog update(@PathVariable Long id, @RequestBody InstrumentAccessLog entity) {
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
