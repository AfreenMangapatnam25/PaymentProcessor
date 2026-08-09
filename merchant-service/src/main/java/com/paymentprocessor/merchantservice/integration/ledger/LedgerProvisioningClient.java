package com.paymentprocessor.merchantservice.integration.ledger;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Provisions merchant ledger accounts in ledger-service when a merchant is activated.
 */
@Component
public class LedgerProvisioningClient {

    private static final Logger log = LoggerFactory.getLogger(LedgerProvisioningClient.class);

    private final RestTemplate restTemplate;
    private final String ledgerBaseUrl;

    public LedgerProvisioningClient(RestTemplateBuilder builder,
                                    @Value("${merchant-service.integration.ledger.base-url:http://localhost:8092}") String ledgerBaseUrl) {
        this.restTemplate = builder.build();
        this.ledgerBaseUrl = ledgerBaseUrl;
    }

    public void provisionMerchantAccounts(String merchantId, String currency) {
        Map<String, Object> request = Map.of(
                "merchantId", merchantId,
                "currency", currency == null || currency.isBlank() ? "USD" : currency
        );
        try {
            restTemplate.postForEntity(
                    ledgerBaseUrl + "/api/v1/accounts/provision-merchant",
                    request,
                    Map.class);
            log.info("Provisioned ledger accounts for merchant {}", merchantId);
        } catch (RestClientException ex) {
            log.error("Failed to provision ledger accounts for merchant {}", merchantId, ex);
            throw ex;
        }
    }
}
