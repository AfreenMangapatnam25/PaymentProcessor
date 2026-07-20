package com.paymentprocessor.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class SendMessageRequest {

    @NotBlank
    private String channel;          // email|sms

    @NotBlank
    private String recipient;        // raw email address or phone number -- never persisted as-is

    @NotBlank
    private String templateKey;

    @NotBlank
    private String locale;

    private Map<String, Object> variables;

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }
}
