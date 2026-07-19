package com.paymentprocessor.notificationservice.dto;

import com.paymentprocessor.notificationservice.entity.Message;
import java.time.Instant;

public class MessageResponse {
    private String id;
    private String channel;
    private String templateId;
    private String locale;
    private String status;
    private String provider;
    private String providerRef;
    private Instant sentAt;
    private Instant createdAt;

    public static MessageResponse from(Message m) {
        MessageResponse r = new MessageResponse();
        r.id = m.getId();
        r.channel = m.getChannel();
        r.templateId = m.getTemplateId();
        r.locale = m.getLocale();
        r.status = m.getStatus();
        r.provider = m.getProvider();
        r.providerRef = m.getProviderRef();
        r.sentAt = m.getSentAt();
        r.createdAt = m.getCreatedAt();
        return r;
    }

    public String getId() { return id; }
    public String getChannel() { return channel; }
    public String getTemplateId() { return templateId; }
    public String getLocale() { return locale; }
    public String getStatus() { return status; }
    public String getProvider() { return provider; }
    public String getProviderRef() { return providerRef; }
    public Instant getSentAt() { return sentAt; }
    public Instant getCreatedAt() { return createdAt; }
}
