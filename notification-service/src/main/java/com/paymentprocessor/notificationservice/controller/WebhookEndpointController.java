package com.paymentprocessor.notificationservice.controller;

import com.paymentprocessor.notificationservice.dto.WebhookEndpointRequest;
import com.paymentprocessor.notificationservice.dto.WebhookEndpointResponse;
import com.paymentprocessor.notificationservice.entity.WebhookEndpoint;
import com.paymentprocessor.notificationservice.service.WebhookEndpointService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhook-endpoints")
public class WebhookEndpointController {

    private final WebhookEndpointService service;

    public WebhookEndpointController(WebhookEndpointService service) {
        this.service = service;
    }

    @GetMapping
    public List<WebhookEndpointResponse> list(@RequestParam(required = false) String merchantId) {
        List<WebhookEndpoint> endpoints = merchantId != null
                ? service.findByMerchantId(merchantId)
                : service.findAll();
        return endpoints.stream().map(WebhookEndpointResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<WebhookEndpointResponse> create(@Valid @RequestBody WebhookEndpointRequest request) {
        WebhookEndpointResponse response = WebhookEndpointResponse.from(service.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public WebhookEndpointResponse get(@PathVariable String id) {
        return service.findById(id)
                .map(WebhookEndpointResponse::from)
                .orElseThrow(() -> new NoSuchElementException("no webhook endpoint with id " + id));
    }

    @PutMapping("/{id}")
    public WebhookEndpointResponse update(@PathVariable String id, @Valid @RequestBody WebhookEndpointRequest request) {
        return WebhookEndpointResponse.from(service.update(id, request));
    }

    @PostMapping("/{id}/disable")
    public WebhookEndpointResponse disable(@PathVariable String id) {
        return WebhookEndpointResponse.from(service.setStatus(id, WebhookEndpoint.STATUS_DISABLED));
    }

    @PostMapping("/{id}/enable")
    public WebhookEndpointResponse enable(@PathVariable String id) {
        return WebhookEndpointResponse.from(service.setStatus(id, WebhookEndpoint.STATUS_ACTIVE));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
