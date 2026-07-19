package com.paymentprocessor.merchantservice.service;

import com.paymentprocessor.merchantservice.common.crypto.SecretHasher;
import com.paymentprocessor.merchantservice.common.enums.WebhookDeliveryStatus;
import com.paymentprocessor.merchantservice.common.enums.WebhookEventType;
import com.paymentprocessor.merchantservice.common.error.ResourceNotFoundException;
import com.paymentprocessor.merchantservice.dto.WebhookCreatedResponse;
import com.paymentprocessor.merchantservice.dto.WebhookRequest;
import com.paymentprocessor.merchantservice.dto.WebhookResponse;
import com.paymentprocessor.merchantservice.entity.Webhook;
import com.paymentprocessor.merchantservice.entity.WebhookDeliveryLog;
import com.paymentprocessor.merchantservice.repository.WebhookDeliveryLogRepository;
import com.paymentprocessor.merchantservice.repository.WebhookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages merchant webhook endpoints and their delivery logs. The HMAC signing secret is
 * generated on registration, stored hashed, and returned exactly once.
 */
@Service
public class WebhookService {

    private static final int SECRET_BYTES = 32;

    private final WebhookRepository repository;
    private final WebhookDeliveryLogRepository deliveryLogRepository;
    private final MerchantService merchantService;
    private final SecretHasher secretHasher;

    public WebhookService(WebhookRepository repository, WebhookDeliveryLogRepository deliveryLogRepository,
                          MerchantService merchantService, SecretHasher secretHasher) {
        this.repository = repository;
        this.deliveryLogRepository = deliveryLogRepository;
        this.merchantService = merchantService;
        this.secretHasher = secretHasher;
    }

    @Transactional(readOnly = true)
    public List<WebhookResponse> list(UUID merchantId) {
        merchantService.assertAccessible(merchantId);
        return repository.findByMerchantId(merchantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public WebhookCreatedResponse register(UUID merchantId, WebhookRequest req) {
        merchantService.assertAccessible(merchantId);
        String secret = secretHasher.generateSecret(SECRET_BYTES);
        Webhook w = new Webhook();
        w.setMerchantId(merchantId);
        w.setSecretHash(secretHasher.hash(secret));
        apply(w, req);
        w = repository.save(w);
        return new WebhookCreatedResponse(toResponse(w), "whsec_" + secret);
    }

    @Transactional
    public WebhookResponse update(UUID merchantId, UUID webhookId, WebhookRequest req) {
        merchantService.assertAccessible(merchantId);
        Webhook w = load(merchantId, webhookId);
        apply(w, req);
        return toResponse(w);
    }

    @Transactional
    public void delete(UUID merchantId, UUID webhookId) {
        merchantService.assertAccessible(merchantId);
        repository.delete(load(merchantId, webhookId));
    }

    /** Records a synthetic delivery attempt so integrators can validate their endpoint wiring. */
    @Transactional
    public void testDelivery(UUID merchantId, UUID webhookId) {
        merchantService.assertAccessible(merchantId);
        Webhook w = load(merchantId, webhookId);
        WebhookDeliveryLog log = new WebhookDeliveryLog();
        log.setWebhookId(w.getId());
        log.setMerchantId(merchantId);
        log.setEventType("webhook.test");
        log.setPayload("{\"type\":\"webhook.test\",\"webhookId\":\"" + w.getId() + "\"}");
        log.setStatus(WebhookDeliveryStatus.PENDING);
        log.setAttempts(0);
        log.setLastAttemptAt(Instant.now());
        deliveryLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<WebhookDeliveryLog> deliveryLogs(UUID merchantId, UUID webhookId) {
        merchantService.assertAccessible(merchantId);
        load(merchantId, webhookId);
        return deliveryLogRepository.findByWebhookIdOrderByCreatedAtDesc(webhookId);
    }

    private void apply(Webhook w, WebhookRequest req) {
        w.setEndpointUrl(req.endpointUrl());
        w.setSubscribedEvents(req.events().stream().map(Enum::name).collect(Collectors.joining(",")));
        w.setActive(req.active());
        if (req.maxRetries() != null) {
            w.setMaxRetries(req.maxRetries());
        }
        if (req.timeoutSeconds() != null) {
            w.setTimeoutSeconds(req.timeoutSeconds());
        }
    }

    private Webhook load(UUID merchantId, UUID webhookId) {
        return repository.findByIdAndMerchantId(webhookId, merchantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Webhook", webhookId));
    }

    private WebhookResponse toResponse(Webhook w) {
        return new WebhookResponse(w.getId(), w.getMerchantId(), w.getEndpointUrl(),
                parseEvents(w.getSubscribedEvents()), w.isActive(), w.getMaxRetries(), w.getTimeoutSeconds());
    }

    private Set<WebhookEventType> parseEvents(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(WebhookEventType::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
