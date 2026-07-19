package com.paymentprocessor.fraudservice.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paymentprocessor.fraudservice.domain.entity.Device;
import com.paymentprocessor.fraudservice.service.DeviceService;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    @GetMapping
    public List<Device> all() {
        return service.findAll();
    }

    @PostMapping
    public Device create(@RequestBody Device entity) {
        return service.save(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Device> get(@PathVariable UUID id) {
        return service.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public Device update(@PathVariable UUID id, @RequestBody Device entity) {
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
