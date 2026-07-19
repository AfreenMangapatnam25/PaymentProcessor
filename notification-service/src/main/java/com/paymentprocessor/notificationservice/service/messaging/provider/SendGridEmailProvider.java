package com.paymentprocessor.notificationservice.service.messaging.provider;

import com.paymentprocessor.notificationservice.config.ProviderProperties;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Real SendGrid integration. Requires notification.providers.sendgrid.api-key
 * (SENDGRID_API_KEY env var) to actually send; without it, send() fails fast
 * with a clear error instead of throwing, so local dev / CI can boot without
 * a live key.
 */
@Component
public class SendGridEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailProvider.class);

    private final ProviderProperties.SendGrid config;

    public SendGridEmailProvider(ProviderProperties providerProperties) {
        this.config = providerProperties.getSendgrid();
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("SENDGRID_API_KEY is not set; email sends will fail until it's configured");
        }
    }

    @Override
    public ProviderResult send(String toEmail, String subject, String htmlOrTextBody) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return ProviderResult.failure("sendgrid not configured: SENDGRID_API_KEY is unset");
        }
        try {
            Email from = new Email(config.getFromEmail(), config.getFromName());
            Email to = new Email(toEmail);
            Content content = new Content("text/html", htmlOrTextBody);
            Mail mail = new Mail(from, subject, to, content);

            SendGrid sg = new SendGrid(config.getApiKey());
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                String messageId = response.getHeaders() != null ? response.getHeaders().get("X-Message-Id") : null;
                return ProviderResult.success(messageId != null ? messageId : "sendgrid-" + response.getStatusCode());
            }
            return ProviderResult.failure("sendgrid returned " + response.getStatusCode() + ": " + response.getBody());
        } catch (Exception e) {
            log.error("sendgrid send failed", e);
            return ProviderResult.failure(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public String name() {
        return "sendgrid";
    }
}
