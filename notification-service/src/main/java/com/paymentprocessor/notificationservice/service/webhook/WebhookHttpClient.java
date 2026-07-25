package com.paymentprocessor.notificationservice.service.webhook;

import com.paymentprocessor.notificationservice.config.DispatcherProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** Thin wrapper around java.net.http.HttpClient for a single webhook POST attempt. */
@Component
public class WebhookHttpClient {

    private final HttpClient httpClient;
    private final DispatcherProperties dispatcherProperties;

    public WebhookHttpClient(HttpClient httpClient, DispatcherProperties dispatcherProperties) {
        this.httpClient = httpClient;
        this.dispatcherProperties = dispatcherProperties;
    }

    public WebhookDeliveryResult post(String url, String body, String signatureHeader,
                                       String eventId, String eventType, long deliveryId) {
        long start = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(dispatcherProperties.getRequestTimeout())
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Signature", signatureHeader)
                    .header("X-Webhook-Delivery-Id", String.valueOf(deliveryId))
                    .header("X-Event-Id", eventId)
                    .header("X-Event-Type", eventType)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int elapsedMs = elapsedMs(start);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return WebhookDeliveryResult.success(response.statusCode(), elapsedMs);
            }
            return WebhookDeliveryResult.httpFailure(response.statusCode(), elapsedMs);
        } catch (java.net.http.HttpTimeoutException e) {
            return WebhookDeliveryResult.transportFailure(elapsedMs(start), "timeout: " + e.getMessage());
        } catch (Exception e) {
            return WebhookDeliveryResult.transportFailure(elapsedMs(start), e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static int elapsedMs(long startNanos) {
        return (int) Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
