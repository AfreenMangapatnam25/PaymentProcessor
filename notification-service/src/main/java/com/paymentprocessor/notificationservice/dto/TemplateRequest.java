package com.paymentprocessor.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;

public class TemplateRequest {

    @NotBlank
    private String key;

    @NotBlank
    private String channel;

    @NotBlank
    private String locale;

    private String subject;

    @NotBlank
    private String body;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
