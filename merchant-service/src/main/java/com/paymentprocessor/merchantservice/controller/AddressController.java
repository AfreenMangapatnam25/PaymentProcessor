package com.paymentprocessor.merchantservice.controller;

import com.paymentprocessor.merchantservice.dto.AddressRequest;
import com.paymentprocessor.merchantservice.dto.AddressResponse;
import com.paymentprocessor.merchantservice.service.AddressService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Business Addresses")
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/addresses")
public class AddressController {

    private final AddressService service;

    public AddressController(AddressService service) {
        this.service = service;
    }

    @GetMapping
    public List<AddressResponse> list(@PathVariable UUID merchantId) {
        return service.list(merchantId);
    }

    @PostMapping
    public ResponseEntity<AddressResponse> add(@PathVariable UUID merchantId,
                                               @Valid @RequestBody AddressRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(merchantId, req));
    }

    @PutMapping("/{addressId}")
    public AddressResponse update(@PathVariable UUID merchantId, @PathVariable UUID addressId,
                                  @Valid @RequestBody AddressRequest req) {
        return service.update(merchantId, addressId, req);
    }

    @PostMapping("/{addressId}/primary")
    public AddressResponse markPrimary(@PathVariable UUID merchantId, @PathVariable UUID addressId) {
        return service.markPrimary(merchantId, addressId);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@PathVariable UUID merchantId, @PathVariable UUID addressId) {
        service.delete(merchantId, addressId);
        return ResponseEntity.noContent().build();
    }
}
