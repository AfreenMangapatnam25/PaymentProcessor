package com.paymentprocessor.fraudservice.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.fraudservice.document.RiskAssessment;
import com.paymentprocessor.fraudservice.service.RiskAssessmentService;

@RestController
@RequestMapping("/api/risk-assessments")
public class RiskAssessmentController {

    private final RiskAssessmentService service;

    public RiskAssessmentController(RiskAssessmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<RiskAssessment> all() {
        return service.findAll();
    }

    @PostMapping
    public RiskAssessment create(@RequestBody RiskAssessment entity) {
        return service.save(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RiskAssessment> get(@PathVariable String id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public RiskAssessment update(@PathVariable String id, @RequestBody RiskAssessment entity) {
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
