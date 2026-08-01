package com.paymentprocessor.disputeservice.integration;

import java.util.Optional;

/**
 * Resolves merchant contact details and notification preferences for outbound alerts.
 */
public interface MerchantClient {

    Optional<MerchantContact> findContact(String merchantId);

    record MerchantContact(
            String merchantId,
            String merchantReference,
            String supportEmail,
            String supportPhone,
            boolean notifyOnChargeback
    ) {
    }
}
