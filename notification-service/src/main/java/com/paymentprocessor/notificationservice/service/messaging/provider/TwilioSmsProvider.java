package com.paymentprocessor.notificationservice.service.messaging.provider;

import com.paymentprocessor.notificationservice.config.ProviderProperties;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Real Twilio integration. Requires notification.providers.twilio.account-sid
 * / auth-token / from-number (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN,
 * TWILIO_FROM_NUMBER env vars). Without them, send() fails fast with a clear
 * error instead of throwing, so local dev / CI can boot without live
 * credentials.
 */
@Component
public class TwilioSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsProvider.class);

    private final ProviderProperties.Twilio config;
    private boolean initialized = false;

    public TwilioSmsProvider(ProviderProperties providerProperties) {
        this.config = providerProperties.getTwilio();
    }

    @PostConstruct
    void init() {
        if (isConfigured()) {
            Twilio.init(config.getAccountSid(), config.getAuthToken());
            initialized = true;
        } else {
            log.warn("TWILIO_ACCOUNT_SID/TWILIO_AUTH_TOKEN/TWILIO_FROM_NUMBER are not fully set; SMS sends will fail until configured");
        }
    }

    private boolean isConfigured() {
        return notBlank(config.getAccountSid()) && notBlank(config.getAuthToken()) && notBlank(config.getFromNumber());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    @Override
    public ProviderResult send(String toPhoneNumber, String body) {
        if (!initialized) {
            return ProviderResult.failure("twilio not configured: account sid/auth token/from number are unset");
        }
        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(config.getFromNumber()),
                    body
            ).create();
            return ProviderResult.success(message.getSid());
        } catch (Exception e) {
            log.error("twilio send failed", e);
            return ProviderResult.failure(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public String name() {
        return "twilio";
    }
}
