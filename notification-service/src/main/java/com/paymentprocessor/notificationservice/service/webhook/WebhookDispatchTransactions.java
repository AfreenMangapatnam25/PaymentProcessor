package com.paymentprocessor.notificationservice.service.webhook;

import com.paymentprocessor.notificationservice.config.DispatcherProperties;
import com.paymentprocessor.notificationservice.entity.WebhookDelivery;
import com.paymentprocessor.notificationservice.entity.WebhookEndpoint;
import com.paymentprocessor.notificationservice.repository.WebhookDeliveryRepository;
import com.paymentprocessor.notificationservice.repository.WebhookEndpointRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * All the transactional persistence steps of the dispatch cycle, pulled out
 * into their own Spring bean.
 *
 * This isn't just organization: Spring's @Transactional relies on a proxy,
 * and a proxy only intercepts calls that come in from *outside* the bean.
 * If these methods lived on WebhookDispatcher and were called via
 * this.claimDue() / this.finalizeSuccess(...), those self-invocations would
 * silently skip the proxy and run with no transaction at all. Keeping them
 * on a separate, injected bean means every call from WebhookDispatcher goes
 * through the real proxy and actually gets the atomicity @Transactional
 * promises (e.g. a delivery's outcome and its endpoint's
 * consecutive_failures update land in the same commit).
 */
@Component
public class WebhookDispatchTransactions {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatchTransactions.class);

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookEndpointRepository endpointRepository;
    private final BackoffPolicy backoffPolicy;
    private final DispatcherProperties properties;

    public WebhookDispatchTransactions(WebhookDeliveryRepository deliveryRepository,
                                        WebhookEndpointRepository endpointRepository,
                                        BackoffPolicy backoffPolicy,
                                        DispatcherProperties properties) {
        this.deliveryRepository = deliveryRepository;
        this.endpointRepository = endpointRepository;
        this.backoffPolicy = backoffPolicy;
        this.properties = properties;
    }

    @Transactional
    public List<WebhookDelivery> claimDue() {
        int reclaimed = deliveryRepository.reclaimStuck();
        if (reclaimed > 0) {
            log.warn("reclaimed {} delivery(ies) stuck in 'delivering' past their claim deadline", reclaimed);
        }
        return deliveryRepository.claimBatch(properties.getBatchSize(), properties.getClaimVisibility().toSeconds());
    }

    @Transactional
    public void finalizeSuccess(WebhookDelivery delivery, WebhookEndpoint endpoint, WebhookDeliveryResult result) {
        delivery.setStatus(WebhookDelivery.STATUS_DELIVERED);
        delivery.setResponseCode(result.getStatusCode());
        delivery.setResponseMs(result.getResponseMs());
        delivery.setError(null);
        delivery.setNextRetryAt(null);
        deliveryRepository.save(delivery);

        if (endpoint.getConsecutiveFailures() != 0) {
            endpoint.setConsecutiveFailures(0);
            endpointRepository.save(endpoint);
        }
    }

    @Transactional
    public void finalizeFailure(WebhookDelivery delivery, WebhookEndpoint endpoint, WebhookDeliveryResult result) {
        delivery.setResponseCode(result.getStatusCode());
        delivery.setResponseMs(result.getResponseMs());
        delivery.setError(result.getError());

        Duration backoff = backoffPolicy.delayFor(delivery.getAttempt());
        if (backoff == null) {
            delivery.setStatus(WebhookDelivery.STATUS_DEAD);
            delivery.setNextRetryAt(null);
            log.warn("delivery {} for endpoint {} exhausted retries and is now dead (last error: {})",
                    delivery.getId(), endpoint.getId(), result.getError());
        } else {
            delivery.setStatus(WebhookDelivery.STATUS_PENDING);
            delivery.setNextRetryAt(Instant.now().plus(backoff));
        }
        deliveryRepository.save(delivery);

        bumpConsecutiveFailures(endpoint);
    }

    @Transactional
    public void finalizeDead(WebhookDelivery delivery, WebhookEndpoint endpoint, String reason) {
        delivery.setStatus(WebhookDelivery.STATUS_DEAD);
        delivery.setNextRetryAt(null);
        delivery.setError(reason);
        deliveryRepository.save(delivery);
        if (endpoint != null) {
            bumpConsecutiveFailures(endpoint);
        }
    }

    private void bumpConsecutiveFailures(WebhookEndpoint endpoint) {
        endpoint.setConsecutiveFailures(endpoint.getConsecutiveFailures() + 1);
        if (endpoint.isActive() && endpoint.getConsecutiveFailures() >= properties.getAutoDisableThreshold()) {
            endpoint.setStatus(WebhookEndpoint.STATUS_AUTO_DISABLED);
            log.warn("endpoint {} auto-disabled after {} consecutive failures",
                    endpoint.getId(), endpoint.getConsecutiveFailures());
        }
        endpointRepository.save(endpoint);
    }
}
