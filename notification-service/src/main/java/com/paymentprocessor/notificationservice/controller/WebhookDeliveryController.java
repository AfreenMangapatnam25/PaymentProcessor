package com.paymentprocessor.notificationservice.controller;

import com.paymentprocessor.notificationservice.dto.WebhookDeliveryResponse;
import com.paymentprocessor.notificationservice.service.WebhookDeliveryService;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhook-deliveries")
public class WebhookDeliveryController {

    private final WebhookDeliveryService service;

    public WebhookDeliveryController(WebhookDeliveryService service) {
        this.service = service;
    }

    @GetMapping
    public List<WebhookDeliveryResponse> list(@RequestParam(required = false) String endpointId) {
        var deliveries = endpointId != null ? service.findByEndpointId(endpointId) : service.findAll();
        return deliveries.stream().map(WebhookDeliveryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public WebhookDeliveryResponse get(@PathVariable Long id) {
        return service.findById(id)
                .map(WebhookDeliveryResponse::from)
                .orElseThrow(() -> new NoSuchElementException("no webhook delivery with id " + id));
    }

    /** Operator-triggered resend of a failed/dead delivery. */
    @PostMapping("/{id}/retry")
    public WebhookDeliveryResponse retry(@PathVariable Long id) {
        return WebhookDeliveryResponse.from(service.retry(id));
    }
}
