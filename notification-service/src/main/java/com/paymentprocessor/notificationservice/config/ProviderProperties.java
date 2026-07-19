package com.paymentprocessor.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** notification.providers.* -- credentials for outbound email/SMS providers. */
@ConfigurationProperties(prefix = "notification.providers")
public class ProviderProperties {

    private final SendGrid sendgrid = new SendGrid();
    private final Twilio twilio = new Twilio();

    public SendGrid getSendgrid() { return sendgrid; }
    public Twilio getTwilio() { return twilio; }

    public static class SendGrid {
        /** Set via SENDGRID_API_KEY env var; never commit a real key. */
        private String apiKey;
        private String fromEmail = "notifications@example.com";
        private String fromName = "Notifications";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getFromEmail() { return fromEmail; }
        public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }
        public String getFromName() { return fromName; }
        public void setFromName(String fromName) { this.fromName = fromName; }
    }

    public static class Twilio {
        /** Set via TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN env vars. */
        private String accountSid;
        private String authToken;
        private String fromNumber;

        public String getAccountSid() { return accountSid; }
        public void setAccountSid(String accountSid) { this.accountSid = accountSid; }
        public String getAuthToken() { return authToken; }
        public void setAuthToken(String authToken) { this.authToken = authToken; }
        public String getFromNumber() { return fromNumber; }
        public void setFromNumber(String fromNumber) { this.fromNumber = fromNumber; }
    }
}
