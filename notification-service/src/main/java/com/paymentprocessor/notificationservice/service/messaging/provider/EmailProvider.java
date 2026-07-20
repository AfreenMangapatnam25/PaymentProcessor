package com.paymentprocessor.notificationservice.service.messaging.provider;

public interface EmailProvider {
    ProviderResult send(String toEmail, String subject, String htmlOrTextBody);

    String name();
}
