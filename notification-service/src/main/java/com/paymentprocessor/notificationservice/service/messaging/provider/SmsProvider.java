package com.paymentprocessor.notificationservice.service.messaging.provider;

public interface SmsProvider {
    ProviderResult send(String toPhoneNumber, String body);

    String name();
}
