package com.paymentprocessor.merchantservice.common.enums;

/** Domain events published by the Merchant Service to downstream consumers. */
public enum DomainEventType {
    MERCHANT_CREATED("MerchantCreated"),
    MERCHANT_ACTIVATED("MerchantActivated"),
    MERCHANT_SUSPENDED("MerchantSuspended"),
    MERCHANT_UPDATED("MerchantUpdated"),
    MERCHANT_STATUS_CHANGED("MerchantStatusChanged"),
    MERCHANT_CONFIGURATION_UPDATED("MerchantConfigurationUpdated");

    private final String eventName;

    DomainEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}
