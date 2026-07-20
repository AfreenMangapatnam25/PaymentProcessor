package com.paymentprocessor.merchantservice.controller;

import com.paymentprocessor.merchantservice.dto.WebhookCreatedResponse;
import com.paymentprocessor.merchantservice.dto.WebhookRequest;
import com.paymentprocessor.merchantservice.dto.WebhookResponse;
import com.paymentprocessor.merchantservice.entity.WebhookDeliveryLog;
import com.paymentprocessor.merchantservice.service.WebhookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Webhooks")
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}/webhooks")
public class WebhookController {

    private final WebhookService service;

    public WebhookController(WebhookService service) {
        this.service = service;
    }

    @GetMapping
    public List<WebhookResponse> list(@PathVariable UUID merchantId) {
        return service.list(merchantId);
    }

    @PostMapping
    public ResponseEntity<WebhookCreatedResponse> register(@PathVariable UUID merchantId,
                                                           @Valid @RequestBody WebhookRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(merchantId, req));
    }

    @PutMapping("/{webhookId}")
    public WebhookResponse update(@PathVariable UUID merchantId, @PathVariable UUID webhookId,
                                  @Valid @RequestBody WebhookRequest req) {
        return service.update(merchantId, webhookId, req);
    }

    @PostMapping("/{webhookId}/test")
    public ResponseEntity<Void> test(@PathVariable UUID merchantId, @PathVariable UUID webhookId) {
        service.testDelivery(merchantId, webhookId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{webhookId}/deliveries")
    public List<WebhookDeliveryLog> deliveries(@PathVariable UUID merchantId, @PathVariable UUID webhookId) {
        return service.deliveryLogs(merchantId, webhookId);
    }

    @DeleteMapping("/{webhookId}")
    public ResponseEntity<Void> delete(@PathVariable UUID merchantId, @PathVariable UUID webhookId) {
        service.delete(merchantId, webhookId);
        return ResponseEntity.noContent().build();
    }
}
