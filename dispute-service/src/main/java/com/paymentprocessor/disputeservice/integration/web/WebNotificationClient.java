package com.paymentprocessor.disputeservice.integration.web;

import com.paymentprocessor.disputeservice.domain.enums.NotificationChannel;
import com.paymentprocessor.disputeservice.domain.enums.NotificationUrgency;
import com.paymentprocessor.disputeservice.integration.MerchantClient;
import com.paymentprocessor.disputeservice.integration.NotificationClient;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Sends dispute notifications to notification-service via {@code POST /api/events}
 * for webhook/dashboard fan-out and {@code POST /api/messages} for direct email/SMS
 * when merchant contact details are available.
 */
@Component
@Primary
public class WebNotificationClient implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(WebNotificationClient.class);
    private static final String API_VERSION = "2026-01-01";

    private final WebClient notificationWebClient;
    private final MerchantClient merchantClient;
    private final String internalApiKey;

    public WebNotificationClient(@Qualifier("notificationServiceWebClient") WebClient notificationWebClient,
                                 MerchantClient merchantClient,
                                 @Value("${notification.service.internal-api-key:}") String internalApiKey) {
        this.notificationWebClient = notificationWebClient;
        this.merchantClient = merchantClient;
        this.internalApiKey = internalApiKey;
    }

    @Override
    public void notifyMerchant(String merchantId, String disputeId, String subject,
                               String body, NotificationUrgency urgency,
                               Set<NotificationChannel> channels) {
        String eventType = mapEventType(subject);
        String eventId = "evt_dispute_" + disputeId + "_" + eventType.replace('.', '_');
        Optional<MerchantClient.MerchantContact> contact = merchantClient.findContact(merchantId);
        String eventMerchantId = contact.map(MerchantClient.MerchantContact::merchantId).orElse(merchantId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("subject", subject);
        payload.put("body", body);
        payload.put("urgency", urgency.name());
        payload.put("channels", channels.stream().map(Enum::name).toList());
        contact.ifPresent(c -> payload.put("merchantReference", c.merchantReference()));

        Map<String, Object> eventRequest = Map.of(
                "id", eventId,
                "merchantId", eventMerchantId,
                "type", eventType,
                "aggregateType", "dispute",
                "aggregateId", disputeId,
                "apiVersion", API_VERSION,
                "payload", payload
        );

        try {
            notificationWebClient.post()
                    .uri("/api/events")
                    .headers(headers -> {
                        if (internalApiKey != null && !internalApiKey.isBlank()) {
                            headers.set("X-Internal-Api-Key", internalApiKey);
                        }
                    })
                    .bodyValue(eventRequest)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            log.error("Failed to ingest dispute notification event merchant={} dispute={}: {}",
                    merchantId, disputeId, ex.toString());
        }

        if (!channels.contains(NotificationChannel.EMAIL) && !channels.contains(NotificationChannel.SMS)) {
            return;
        }

        contact.ifPresentOrElse(c -> {
            if (!c.notifyOnChargeback()) {
                log.info("Skipping direct email/SMS for merchant {} (notifyOnChargeback=false)", merchantId);
                return;
            }
            if (channels.contains(NotificationChannel.EMAIL) && c.supportEmail() != null) {
                sendMessage("email", c.supportEmail(), subject, body, disputeId);
            }
            if (channels.contains(NotificationChannel.SMS) && c.supportPhone() != null) {
                sendMessage("sms", c.supportPhone(), subject, body, disputeId);
            }
        }, () -> log.warn("No merchant contact found for {}; skipping direct email/SMS", merchantId));
    }

    private void sendMessage(String channel, String recipient, String subject, String body, String disputeId) {
        Map<String, Object> variables = Map.of(
                "subject", subject,
                "body", body,
                "disputeId", disputeId
        );
        Map<String, Object> request = Map.of(
                "channel", channel,
                "recipient", recipient,
                "templateKey", "dispute.notification",
                "locale", "en-US",
                "variables", variables
        );
        try {
            notificationWebClient.post()
                    .uri("/api/messages")
                    .headers(headers -> {
                        if (internalApiKey != null && !internalApiKey.isBlank()) {
                            headers.set("X-Internal-Api-Key", internalApiKey);
                        }
                    })
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            log.error("Failed to send {} notification to {} for dispute {}: {}",
                    channel, recipient, disputeId, ex.toString());
        }
    }

    private static String mapEventType(String subject) {
        return switch (subject) {
            case "Chargeback received" -> "chargeback.received";
            case "Evidence requested" -> "dispute.evidence_requested";
            case "Pre-arbitration required" -> "dispute.pre_arbitration";
            case "Dispute won" -> "dispute.won";
            case "Dispute lost" -> "dispute.lost";
            case "Deadline approaching" -> "dispute.deadline_approaching";
            default -> "dispute.notification";
        };
    }
}
