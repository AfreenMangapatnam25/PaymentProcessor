package com.paymentprocessor.notificationservice.controller;

import com.paymentprocessor.notificationservice.dto.SuppressionRequest;
import com.paymentprocessor.notificationservice.dto.SuppressionResponse;
import com.paymentprocessor.notificationservice.service.messaging.SuppressionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suppressions")
public class SuppressionController {

    private static final Logger log = LoggerFactory.getLogger(SuppressionController.class);

    // SendGrid Event Webhook event types that should suppress future sends.
    private static final Set<String> SENDGRID_SUPPRESSING_EVENTS = Set.of("bounce", "dropped", "spamreport", "unsubscribe");
    // Twilio delivery statuses indicating the number is undeliverable.
    private static final Set<String> TWILIO_SUPPRESSING_STATUSES = Set.of("failed", "undelivered");

    private final SuppressionService service;

    public SuppressionController(SuppressionService service) {
        this.service = service;
    }

    @GetMapping
    public List<SuppressionResponse> all() {
        return service.findAll().stream().map(SuppressionResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<SuppressionResponse> create(@Valid @RequestBody SuppressionRequest request) {
        SuppressionResponse response =
                SuppressionResponse.from(service.suppress(request.getChannel(), request.getRecipient(), request.getReason()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/check")
    public Map<String, Boolean> check(@RequestParam String channel, @RequestParam String recipient) {
        return Map.of("suppressed", service.isSuppressed(channel, recipient));
    }

    /**
     * SendGrid Event Webhook receiver: https://www.twilio.com/docs/sendgrid/for-developers/tracking-events/event
     * Payload is a JSON array of event objects. In production this should also verify
     * SendGrid's Ed25519 request signature before trusting the payload.
     */
    @PostMapping("/webhooks/sendgrid")
    public ResponseEntity<Void> sendgridWebhook(@RequestBody List<Map<String, Object>> events) {
        for (Map<String, Object> event : events) {
            String email = (String) event.get("email");
            String type = (String) event.get("event");
            if (email == null || type == null) {
                continue;
            }
            if (SENDGRID_SUPPRESSING_EVENTS.contains(type)) {
                service.suppress("email", email, mapSendgridReason(type));
                log.info("suppressed email recipient due to SendGrid event: {}", type);
            }
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Twilio status callback receiver (application/x-www-form-urlencoded):
     * https://www.twilio.com/docs/messaging/guides/track-outbound-message-status
     * In production this should also verify the X-Twilio-Signature header.
     */
    @PostMapping("/webhooks/twilio")
    public ResponseEntity<Void> twilioWebhook(@RequestParam Map<String, String> params) {
        String to = params.get("To");
        String status = params.get("MessageStatus");
        if (to != null && status != null && TWILIO_SUPPRESSING_STATUSES.contains(status.toLowerCase())) {
            service.suppress("sms", to, "bounce");
            log.info("suppressed sms recipient due to Twilio status: {}", status);
        }
        return ResponseEntity.ok().build();
    }

    private String mapSendgridReason(String sendgridEventType) {
        return switch (sendgridEventType) {
            case "spamreport" -> "complaint";
            case "unsubscribe" -> "unsubscribe";
            default -> "bounce";
        };
    }
}
