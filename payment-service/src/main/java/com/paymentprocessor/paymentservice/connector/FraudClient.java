package com.paymentprocessor.paymentservice.connector;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.paymentprocessor.paymentservice.config.ConnectorProperties;
import com.paymentprocessor.paymentservice.config.HttpClientConfig.RestClientFactory;

/**
 * Thin client over the Fraud / Risk service. Evaluated before every card
 * authorization. If risk scoring is disabled or the service is unreachable the
 * client fails open (approve) so a risk outage does not halt all payments; tune
 * this policy per your risk appetite.
 */
@Component
public class FraudClient {

    private static final Logger log = LoggerFactory.getLogger(FraudClient.class);

    private final RestClient client;
    private final boolean enabled;

    public FraudClient(ConnectorProperties props, RestClientFactory factory) {
        this.client = factory.build(props.getFraud());
        this.enabled = props.getFraud().isEnabled();
    }

    public FraudDecision evaluate(String intentId, String merchantId, long amountMinor,
                                  String currency, String instrumentToken) {
        if (!enabled) {
            return FraudDecision.approve();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("intentId", intentId);
        body.put("merchantId", merchantId);
        body.put("amountMinor", amountMinor);
        body.put("currency", currency);
        body.put("instrumentToken", instrumentToken);
        try {
            FraudResponse r = client.post().uri("/v1/evaluate")
                    .body(body).retrieve().body(FraudResponse.class);
            if (r == null) return FraudDecision.approve();
            boolean approved = r.decision() == null || !r.decision().equalsIgnoreCase("BLOCK");
            boolean threeDs = r.requireThreeDs() != null && r.requireThreeDs();
            return new FraudDecision(approved, threeDs, r.score() == null ? 0 : r.score(),
                    r.reason() == null ? "" : r.reason());
        } catch (Exception e) {
            log.warn("Fraud evaluation unavailable, failing open: {}", e.getMessage());
            return FraudDecision.approve();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FraudResponse(String decision, Boolean requireThreeDs, Integer score, String reason) {
    }
}
