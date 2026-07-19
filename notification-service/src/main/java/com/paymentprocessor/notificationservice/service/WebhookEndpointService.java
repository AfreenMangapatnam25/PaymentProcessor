package com.paymentprocessor.notificationservice.service;

import com.paymentprocessor.notificationservice.dto.WebhookEndpointRequest;
import com.paymentprocessor.notificationservice.entity.WebhookEndpoint;
import com.paymentprocessor.notificationservice.repository.WebhookEndpointRepository;
import com.paymentprocessor.notificationservice.util.IdGenerator;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookEndpointService {

    private final WebhookEndpointRepository repository;

    public WebhookEndpointService(WebhookEndpointRepository repository) {
        this.repository = repository;
    }

    public List<WebhookEndpoint> findAll() {
        return repository.findAll();
    }

    public List<WebhookEndpoint> findByMerchantId(String merchantId) {
        return repository.findByMerchantId(merchantId);
    }

    public Optional<WebhookEndpoint> findById(String id) {
        return repository.findById(id);
    }

    @Transactional
    public WebhookEndpoint create(WebhookEndpointRequest request) {
        WebhookEndpoint endpoint = new WebhookEndpoint();
        endpoint.setId(IdGenerator.generate("ep"));
        endpoint.setMerchantId(request.getMerchantId());
        endpoint.setUrl(request.getUrl());
        // The secret itself never passes through this API; secret_ref just names
        // a KMS entry (see SecretResolver) that this endpoint's signature key lives under.
        endpoint.setSecretRef("kms://webhook-secrets/" + endpoint.getId());
        endpoint.setSubscribedTypes(request.getSubscribedTypes());
        endpoint.setApiVersion(request.getApiVersion());
        endpoint.setStatus(WebhookEndpoint.STATUS_ACTIVE);
        endpoint.setConsecutiveFailures(0);
        endpoint.setCreatedAt(Instant.now());
        return repository.save(endpoint);
    }

    @Transactional
    public WebhookEndpoint update(String id, WebhookEndpointRequest request) {
        WebhookEndpoint endpoint = getOrThrow(id);
        endpoint.setUrl(request.getUrl());
        endpoint.setSubscribedTypes(request.getSubscribedTypes());
        endpoint.setApiVersion(request.getApiVersion());
        return repository.save(endpoint);
    }

    @Transactional
    public WebhookEndpoint setStatus(String id, String status) {
        WebhookEndpoint endpoint = getOrThrow(id);
        endpoint.setStatus(status);
        if (WebhookEndpoint.STATUS_ACTIVE.equals(status)) {
            endpoint.setConsecutiveFailures(0);
        }
        return repository.save(endpoint);
    }

    @Transactional
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    private WebhookEndpoint getOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("no webhook endpoint with id " + id));
    }
}
