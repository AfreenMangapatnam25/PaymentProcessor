package com.paymentprocessor.notificationservice.service;

import com.paymentprocessor.notificationservice.entity.WebhookDelivery;
import com.paymentprocessor.notificationservice.repository.WebhookDeliveryRepository;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookDeliveryService {

    private final WebhookDeliveryRepository repository;

    public WebhookDeliveryService(WebhookDeliveryRepository repository) {
        this.repository = repository;
    }

    public List<WebhookDelivery> findAll() {
        return repository.findAll();
    }

    public List<WebhookDelivery> findByEndpointId(String endpointId) {
        return repository.findByEndpointId(endpointId);
    }

    public Optional<WebhookDelivery> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * Operator-triggered retry for a delivery that's 'failed' or 'dead'.
     * Resets the attempt counter so it gets the full backoff schedule again,
     * and makes it immediately due; the dispatcher's ordinary poll picks it
     * up (still subject to the same per-endpoint ordering guard as any other
     * pending row).
     */
    @Transactional
    public WebhookDelivery retry(Long id) {
        WebhookDelivery delivery = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("no webhook delivery with id " + id));
        if (WebhookDelivery.STATUS_DELIVERED.equals(delivery.getStatus())) {
            throw new IllegalStateException("delivery " + id + " already delivered");
        }
        delivery.setStatus(WebhookDelivery.STATUS_PENDING);
        delivery.setAttempt(0);
        delivery.setNextRetryAt(Instant.now());
        delivery.setError(null);
        return repository.save(delivery);
    }
}
